package com.example.network

import com.example.data.CustomLayout
import com.example.data.CustomLayoutRepository
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest

/**
 * Hardened embedded HTTP server running directly on the Android phone (port 8765):
 * - Generates a fresh 6-digit session PIN on every start (required in X-Session-Pin header)
 * - Enforces socket read timeout (5 s) and strict 512 KB request body limit to prevent DoS/OOM
 */
class LocalWebLayoutServer(
    private val layoutRepository: CustomLayoutRepository,
    private val onLayoutImported: (String) -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    private val _serverUrl = MutableStateFlow<String?>(null)
    val serverUrl: StateFlow<String?> = _serverUrl.asStateFlow()

    private val _sessionPin = MutableStateFlow<String?>(null)
    val sessionPin: StateFlow<String?> = _sessionPin.asStateFlow()

    fun start(port: Int = 8765) {
        if (acceptJob?.isActive == true) return
        val ip = findLocalIpv4Address() ?: "127.0.0.1"
        val pin = SettingsRepository.generateSixDigitPin()
        acceptJob = scope.launch {
            runCatching {
                val ss = ServerSocket(port)
                serverSocket = ss
                _sessionPin.value = pin
                _serverUrl.value = "http://$ip:$port"
                while (isActive && !ss.isClosed) {
                    val client = runCatching { ss.accept() }.getOrNull() ?: break
                    launch { handleClient(client) }
                }
            }.onFailure {
                _serverUrl.value = null
                _sessionPin.value = null
            }
        }
    }

    fun stop() {
        acceptJob?.cancel()
        acceptJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
        _serverUrl.value = null
        _sessionPin.value = null
    }

    private suspend fun handleClient(socket: Socket) {
        runCatching {
            socket.use { s ->
                s.soTimeout = SOCKET_TIMEOUT_MS
                val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                val requestLine = reader.readLine() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return
                val method = parts[0]
                val path = parts[1].substringBefore("?")

                var contentLength = 0
                var providedPin = ""
                var headerCount = 0
                while (headerCount < MAX_HEADERS) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) break
                    headerCount++
                    if (line.startsWith("Content-Length:", ignoreCase = true)) {
                        contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                    } else if (line.startsWith("$PIN_HEADER:", ignoreCase = true)) {
                        providedPin = line.substringAfter(":").trim()
                    }
                }

                val out = s.getOutputStream()
                if (contentLength < 0 || contentLength > MAX_BODY_BYTES) {
                    writeHttpResponse(out, "413 Payload Too Large", "application/json", """{"ok":false,"error":"payload_too_large"}""")
                    return
                }

                val body = if (contentLength > 0) {
                    val chars = CharArray(contentLength)
                    var read = 0
                    while (read < contentLength) {
                        val r = reader.read(chars, read, contentLength - read)
                        if (r <= 0) break
                        read += r
                    }
                    String(chars, 0, read)
                } else ""

                when {
                    path == "/api/layouts" -> {
                        val expectedPin = _sessionPin.value.orEmpty()
                        if (!isPinAuthorized(providedPin, expectedPin)) {
                            writeHttpResponse(out, "401 Unauthorized", "application/json", """{"ok":false,"error":"invalid_pin"}""")
                            return
                        }
                        if (method == "GET") {
                            val layouts: List<CustomLayout> = layoutRepository.allLayouts.first()
                            val jsonArr = layouts.joinToString(",", "[", "]") { layoutRepository.exportToJson(it) }
                            writeHttpResponse(out, "200 OK", "application/json; charset=utf-8", jsonArr)
                        } else if (method == "POST") {
                            val res = layoutRepository.importFromJson(body)
                            if (res.isSuccess) {
                                onLayoutImported("Layout autenticado recebido do navegador do PC!")
                                writeHttpResponse(out, "200 OK", "application/json", """{"ok":true}""")
                            } else {
                                writeHttpResponse(out, "400 Bad Request", "application/json", """{"ok":false,"error":"invalid_json"}""")
                            }
                        } else {
                            writeHttpResponse(out, "405 Method Not Allowed", "application/json", """{"ok":false}""")
                        }
                    }
                    method == "GET" && (path == "/" || path == "/index.html") -> {
                        writeHttpResponse(out, "200 OK", "text/html; charset=utf-8", EDITOR_HTML)
                    }
                    else -> {
                        writeHttpResponse(out, "404 Not Found", "text/plain", "Not Found")
                    }
                }
            }
        }
    }

    private fun writeHttpResponse(out: java.io.OutputStream, status: String, contentType: String, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $status\r\n" +
            "Content-Type: $contentType\r\n" +
            "Content-Length: ${bytes.size}\r\n" +
            "X-Content-Type-Options: nosniff\r\n" +
            "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun findLocalIpv4Address(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces()?.toList()
            ?.flatMap { it.inetAddresses.toList() }
            ?.firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
            ?.hostAddress
    }.getOrNull()

    companion object {
        const val PIN_HEADER = "X-Session-Pin"
        const val MAX_BODY_BYTES = 512 * 1024 // 512 KB
        const val SOCKET_TIMEOUT_MS = 5000
        private const val MAX_HEADERS = 64

        fun isPinAuthorized(provided: String, expected: String): Boolean {
            if (provided.isBlank() || expected.isBlank()) return false
            return MessageDigest.isEqual(
                provided.trim().toByteArray(Charsets.UTF_8),
                expected.trim().toByteArray(Charsets.UTF_8),
            )
        }

        private val EDITOR_HTML = """
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<title>Controle BT — Editor Web Seguro de Layouts</title>
<style>
  body { background:#090d16; color:#f1f5f9; font-family:system-ui,sans-serif; max-width:860px; margin:24px auto; padding:0 16px; }
  h1 { color:#22d3ee; margin-bottom:4px; }
  input { background:#0f172a; color:#f8fafc; border:1px solid #22d3ee88; border-radius:10px; padding:10px 14px; font-size:15px; letter-spacing:2px; width:170px; margin-right:8px; }
  textarea { width:100%; height:320px; background:#111827; color:#a5f3fc; border:1px solid #22d3ee55; border-radius:12px; padding:12px; font-family:monospace; font-size:13px; box-sizing:border-box; }
  button { background:#22d3ee; color:#020617; border:none; padding:10px 18px; border-radius:10px; font-weight:700; cursor:pointer; margin:8px 8px 8px 0; }
  .card { background:#111827; border:1px solid #1e293b; border-radius:14px; padding:14px; margin:12px 0; }
</style>
</head>
<body>
  <h1>Controle BT — Editor Seguro de Layouts</h1>
  <p>Digite o PIN de 6 dígitos exibido no celular para carregar ou enviar layouts na rede local.</p>
  <div class="card">
    <input id="pinBox" type="password" maxlength="6" placeholder="PIN (6 dígitos)" />
    <button onclick="loadLayouts()">Carregar Layouts do Celular</button>
    <button onclick="saveLayout()">Enviar JSON para o Celular</button>
    <div id="status" style="margin-top:8px;color:#34d399;"></div>
  </div>
  <textarea id="jsonBox">{
  "name": "Meu Layout Web",
  "description": "Criado pelo navegador do PC",
  "category": "Personalizado",
  "widgets": [
    {"id": "w1", "label": "Touchpad", "type": "TOUCHPAD", "colSpan": 4, "heightUnits": 2, "keyValue": "", "macroSteps": []},
    {"id": "w2", "label": "Copiar", "type": "SHORTCUT", "colSpan": 2, "heightUnits": 1, "keyValue": "CTRL+C", "macroSteps": []},
    {"id": "w3", "label": "Colar", "type": "SHORTCUT", "colSpan": 2, "heightUnits": 1, "keyValue": "CTRL+V", "macroSteps": []}
  ]
}</textarea>
<script>
function getPin() { return document.getElementById('pinBox').value.trim(); }
async function loadLayouts() {
  const r = await fetch('/api/layouts', { headers: { 'X-Session-Pin': getPin() } });
  if (r.status === 401) { document.getElementById('status').innerText = 'PIN inválido! Confira o código no celular.'; return; }
  const arr = await r.json();
  if (arr.length) document.getElementById('jsonBox').value = JSON.stringify(arr[0], null, 2);
  document.getElementById('status').innerText = arr.length + ' layout(s) carregado(s) com segurança.';
}
async function saveLayout() {
  const r = await fetch('/api/layouts', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Session-Pin': getPin() },
    body: document.getElementById('jsonBox').value
  });
  if (r.status === 401) { document.getElementById('status').innerText = 'PIN inválido! Confira o código no celular.'; return; }
  document.getElementById('status').innerText = r.ok ? 'Enviado com sucesso para o Controle BT!' : 'JSON inválido ou muito grande.';
}
</script>
</body>
</html>
        """.trimIndent()
    }
}
