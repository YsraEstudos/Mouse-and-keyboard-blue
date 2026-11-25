package com.example.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class PcStatusDto(
    val activeApp: String = "",
    val windowTitle: String = "",
    val mediaTitle: String = "",
    val mediaArtist: String = "",
    val mediaPositionSec: Int = 0,
    val mediaDurationSec: Int = 0,
    val mediaPlaying: Boolean = false,
    val cpuPercent: Int = 0,
    val ramPercent: Int = 0,
    val batteryPercent: Int = -1,
    val clipboardText: String = "",
)

@JsonClass(generateAdapter = true)
data class RemoteFileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
)

/**
 * Genuine Wake-on-LAN (WoL) UDP Magic Packet sender (6 x 0xFF + 16 x 6-byte MAC).
 */
object WakeOnLanSender {
    suspend fun sendMagicPacket(macAddress: String, broadcastIp: String = "255.255.255.255", port: Int = 9): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleaned = macAddress.trim().replace("-", ":").uppercase()
                val hexParts = cleaned.split(":")
                require(hexParts.size == 6) { "MAC inválido. Use o formato AA:BB:CC:DD:EE:FF" }
                val macBytes = ByteArray(6) { idx -> hexParts[idx].toInt(16).toByte() }

                val packetBytes = ByteArray(6 + 16 * 6)
                for (i in 0 until 6) packetBytes[i] = 0xFF.toByte()
                for (rep in 0 until 16) {
                    System.arraycopy(macBytes, 0, packetBytes, 6 + rep * 6, 6)
                }

                val address = InetAddress.getByName(broadcastIp.ifBlank { "255.255.255.255" })
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    socket.send(DatagramPacket(packetBytes, packetBytes.size, address, port))
                }
            }
        }
}

/**
 * Hardened Optional Wi-Fi / USB LAN PC Companion HTTP Client:
 * - Enforces LAN/Loopback-only host validation (blocks sending HTTP or tokens to public internet hosts)
 * - Authenticates every request with X-ControleBT-Token header
 * - Downsamples large screen bitmaps safely to avoid RAM spikes / OOM
 */
class PcCompanionClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(3500, TimeUnit.MILLISECONDS)
        .writeTimeout(10000, TimeUnit.MILLISECONDS)
        .build()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val statusAdapter = moshi.adapter(PcStatusDto::class.java)
    private val filesType = Types.newParameterizedType(List::class.java, RemoteFileEntry::class.java)
    private val filesAdapter = moshi.adapter<List<RemoteFileEntry>>(filesType)

    private fun validatedBaseUrl(host: String, port: Int): String {
        val cleanHost = host.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        require(isLocalOrPrivateHost(cleanHost)) {
            "Por segurança, o Agente PC aceita apenas IPs de rede local (ex: 192.168.x.x, 10.x.x.x ou 127.0.0.1)"
        }
        val safePort = port.coerceIn(1024, 65535)
        return "http://$cleanHost:$safePort"
    }

    private fun Request.Builder.withAuth(token: String): Request.Builder {
        if (token.isNotBlank()) {
            header(HEADER_TOKEN, token.trim())
        }
        return this
    }

    suspend fun fetchStatus(host: String, port: Int, token: String): Result<PcStatusDto> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("${validatedBaseUrl(host, port)}/status")
                .withAuth(token)
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.code == 401) error("PIN/Token recusado pelo Agente PC")
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val body = resp.body?.string().orEmpty()
                statusAdapter.fromJson(body) ?: PcStatusDto()
            }
        }
    }

    suspend fun pushClipboard(host: String, port: Int, token: String, text: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url("${validatedBaseUrl(host, port)}/clipboard")
                    .withAuth(token)
                    .post(text.toRequestBody("text/plain; charset=utf-8".toMediaType()))
                    .build()
                client.newCall(req).execute().use {
                    if (it.code == 401) error("PIN/Token inválido")
                    if (!it.isSuccessful) error("HTTP ${it.code}")
                }
            }
        }

    suspend fun pullClipboard(host: String, port: Int, token: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("${validatedBaseUrl(host, port)}/clipboard")
                .withAuth(token)
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.code == 401) error("PIN/Token inválido")
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                resp.body?.string().orEmpty()
            }
        }
    }

    suspend fun fetchScreenMagnifier(
        host: String,
        port: Int,
        token: String,
        fullScreen: Boolean = false,
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = if (fullScreen) "/screen/full" else "/screen/magnifier"
            val req = Request.Builder()
                .url("${validatedBaseUrl(host, port)}$endpoint")
                .withAuth(token)
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.code == 401) error("PIN/Token inválido")
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val bytes = resp.body?.bytes() ?: error("Sem imagem")
                decodeSampledBitmap(bytes, maxDimPx = 1280) ?: error("JPEG inválido")
            }
        }
    }

    private fun decodeSampledBitmap(bytes: ByteArray, maxDimPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxDimPx || bounds.outHeight / sampleSize > maxDimPx) {
            sampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    suspend fun seekMedia(host: String, port: Int, token: String, positionSec: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url("${validatedBaseUrl(host, port)}/media/seek?pos=$positionSec")
                    .withAuth(token)
                    .post("".toRequestBody(null))
                    .build()
                client.newCall(req).execute().use { if (!it.isSuccessful) error("HTTP ${it.code}") }
            }
        }

    suspend fun listRemoteFiles(host: String, port: Int, token: String, path: String = ""): Result<List<RemoteFileEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
                val req = Request.Builder()
                    .url("${validatedBaseUrl(host, port)}/files?path=$encodedPath")
                    .withAuth(token)
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.code == 401) error("PIN/Token inválido")
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    val body = resp.body?.string().orEmpty()
                    filesAdapter.fromJson(body).orEmpty()
                }
            }
        }

    suspend fun openRemoteFile(host: String, port: Int, token: String, path: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url("${validatedBaseUrl(host, port)}/files/open")
                    .withAuth(token)
                    .post(path.toRequestBody("text/plain; charset=utf-8".toMediaType()))
                    .build()
                client.newCall(req).execute().use {
                    if (it.code == 401) error("PIN/Token inválido")
                    if (!it.isSuccessful) error("HTTP ${it.code}")
                }
            }
        }

    suspend fun uploadFileToPc(
        host: String,
        port: Int,
        token: String,
        fileName: String,
        bytes: ByteArray,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val safeName = fileName.substringAfterLast('/').substringAfterLast('\\').ifBlank { "arquivo_controle_bt" }
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    safeName,
                    bytes.toRequestBody("application/octet-stream".toMediaType()),
                )
                .build()
            val req = Request.Builder()
                .url("${validatedBaseUrl(host, port)}/files/upload")
                .withAuth(token)
                .post(multipart)
                .build()
            client.newCall(req).execute().use {
                if (it.code == 401) error("PIN/Token inválido")
                if (!it.isSuccessful) error("HTTP ${it.code}")
            }
        }
    }

    companion object {
        const val HEADER_TOKEN = "X-ControleBT-Token"

        /**
         * Validates that host is strictly localhost, .local mDNS, or an RFC1918 private LAN IPv4 address.
         * Prevents accidentally leaking the auth token or clipboard over the public internet in cleartext.
         */
        fun isLocalOrPrivateHost(rawHost: String): Boolean {
            val h = rawHost.trim().lowercase()
            if (h.isEmpty()) return false
            if (h == "localhost" || h == "127.0.0.1" || h == "::1" || h.endsWith(".local")) return true
            val parts = h.split(".")
            if (parts.size != 4) return false
            val nums = parts.map { it.toIntOrNull() ?: return false }
            if (nums.any { it !in 0..255 }) return false
            return when {
                nums[0] == 10 -> true
                nums[0] == 127 -> true
                nums[0] == 192 && nums[1] == 168 -> true
                nums[0] == 172 && nums[1] in 16..31 -> true
                nums[0] == 169 && nums[1] == 254 -> true
                else -> false
            }
        }

        fun buildPythonCompanionScript(token: String): String {
            val safeToken = token.ifBlank { "123456" }.replace("\"", "").replace("\\", "")
            return """
# Controle BT - Agente Opcional PC Seguro (Python 3)
# Instale: pip install flask psutil pyperclip pillow pyautogui
# Execute: python controle_bt_agent.py
from flask import Flask, request, jsonify, Response, abort
import hmac, io, os, re, subprocess, sys
import psutil, pyperclip, pyautogui

APP_TOKEN = "$safeToken"
HOME_DIR = os.path.realpath(os.path.expanduser("~"))
DOWNLOADS_DIR = os.path.realpath(os.path.join(HOME_DIR, "Downloads"))
os.makedirs(DOWNLOADS_DIR, exist_ok=True)

app = Flask(__name__)
app.config["MAX_CONTENT_LENGTH"] = 50 * 1024 * 1024  # 50 MB max upload

def _resolve_inside_home(raw_path: str) -> str:
    candidate = os.path.realpath(raw_path or HOME_DIR)
    if os.path.commonpath([HOME_DIR, candidate]) != HOME_DIR:
        abort(403, "Acesso fora da pasta do usuário bloqueado")
    return candidate

@app.before_request
def require_token():
    provided = request.headers.get("X-ControleBT-Token", "")
    if not hmac.compare_digest(provided, APP_TOKEN):
        abort(401, "Token inválido ou ausente")

@app.get("/status")
def status():
    bat = psutil.sensors_battery()
    return jsonify({
        "activeApp": "Desktop",
        "windowTitle": "",
        "mediaTitle": "",
        "mediaArtist": "",
        "mediaPositionSec": 0,
        "mediaDurationSec": 0,
        "mediaPlaying": False,
        "cpuPercent": int(psutil.cpu_percent()),
        "ramPercent": int(psutil.virtual_memory().percent),
        "batteryPercent": int(bat.percent) if bat else -1,
        "clipboardText": (pyperclip.paste() or "")[:400],
    })

@app.route("/clipboard", methods=["GET", "POST"])
def clipboard():
    if request.method == "POST":
        pyperclip.copy(request.get_data(as_text=True))
        return "OK"
    return pyperclip.paste() or ""

@app.get("/screen/magnifier")
def magnifier():
    x, y = pyautogui.position()
    img = pyautogui.screenshot(region=(max(0, x - 180), max(0, y - 120), 360, 240))
    buf = io.BytesIO(); img.save(buf, format="JPEG", quality=72)
    return Response(buf.getvalue(), mimetype="image/jpeg")

@app.get("/screen/full")
def full_screen():
    img = pyautogui.screenshot()
    img.thumbnail((1280, 720))
    buf = io.BytesIO(); img.save(buf, format="JPEG", quality=68)
    return Response(buf.getvalue(), mimetype="image/jpeg")

@app.get("/files")
def list_files():
    base = _resolve_inside_home(request.args.get("path") or HOME_DIR)
    out = []
    for name in sorted(os.listdir(base))[:80]:
        full = os.path.join(base, name)
        out.append({
            "name": name,
            "path": full,
            "isDirectory": os.path.isdir(full),
            "sizeBytes": os.path.getsize(full) if os.path.isfile(full) else 0,
        })
    return jsonify(out)

@app.post("/files/open")
def open_file():
    target = _resolve_inside_home(request.get_data(as_text=True).strip())
    if not os.path.exists(target):
        abort(404)
    if os.name == "nt":
        os.startfile(target)
    elif sys.platform == "darwin":
        subprocess.Popen(["open", target], shell=False)
    else:
        subprocess.Popen(["xdg-open", target], shell=False)
    return "OK"

@app.post("/files/upload")
def upload():
    f = request.files.get("file")
    if not f:
        abort(400)
    raw_name = os.path.basename(f.filename or "arquivo_controle_bt")
    safe_name = re.sub(r"[^A-Za-z0-9._ -]", "_", raw_name).strip(". ") or "arquivo_controle_bt"
    dest = os.path.realpath(os.path.join(DOWNLOADS_DIR, safe_name))
    if os.path.commonpath([DOWNLOADS_DIR, dest]) != DOWNLOADS_DIR:
        abort(403)
    f.save(dest)
    return "OK"

print(f"Controle BT Agente Seguro ativo na porta 8766 | PIN: {APP_TOKEN}")
app.run(host="0.0.0.0", port=8766)
            """.trimIndent()
        }

        val PYTHON_COMPANION_SCRIPT: String
            get() = buildPythonCompanionScript("123456")
    }
}
