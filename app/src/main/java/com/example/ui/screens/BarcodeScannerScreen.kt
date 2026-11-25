package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppSettings
import com.example.system.ThermalHealthLevel
import com.example.ui.components.RemoteSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusWarning
import com.example.viewmodel.RemoteViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Composable
fun BarcodeScannerScreen(
    viewModel: RemoteViewModel,
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanHistory by viewModel.scanHistory.collectAsStateWithLifecycle()
    val deviceProfile by viewModel.deviceProfile.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var ocrMode by remember { mutableStateOf(false) }
    var ocrFrozen by remember { mutableStateOf(false) }
    var liveOcrText by remember { mutableStateOf("") }
    var liveOcrLines by remember { mutableStateOf<List<String>>(emptyList()) }
    var torchEnabled by remember { mutableStateOf(false) }
    var linearZoom by remember { mutableFloatStateOf(0.05f) }
    var minZoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(4f) }
    var activeZoomRatio by remember { mutableFloatStateOf(1f) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Mode Switcher: QR / Barras vs OCR Texto de Papel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = !ocrMode,
                onClick = { ocrMode = false; ocrFrozen = false },
                leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("QR / Barras") },
                modifier = Modifier.weight(1f).testTag("scanner_mode_barcode"),
            )
            FilterChip(
                selected = ocrMode,
                onClick = { ocrMode = true; ocrFrozen = false },
                leadingIcon = { Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Leitor OCR (Texto)") },
                modifier = Modifier.weight(1f).testTag("scanner_mode_ocr"),
            )
        }

        if (!hasCameraPermission) {
            RemoteSurface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Permissão de câmera", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(14.dp))
                    Text("Leitor Óptico QR, Barras e OCR", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Conceda acesso à câmera para escanear QR Codes, códigos de barras e reconhecer texto impresso (boletos, senhas Wi-Fi, livros) direto no celular.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.testTag("scanner_grant_camera_button"),
                    ) {
                        Text("Permitir Câmera")
                    }
                }
            }
        } else {
            // Live CameraX Preview + Dual ML Kit Analyzer (Barcode & On-Device OCR Text)
            RemoteSurface(
                modifier = Modifier.fillMaxWidth().weight(1.15f),
                shape = RoundedCornerShape(20.dp),
                highlighted = true,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraDualScannerPreview(
                        ocrMode = ocrMode,
                        ocrPaused = ocrFrozen,
                        onCameraReady = { cam ->
                            boundCamera = cam
                            cam.cameraInfo.zoomState.value?.let { zs ->
                                minZoomRatio = zs.minZoomRatio
                                maxZoomRatio = zs.maxZoomRatio
                                activeZoomRatio = zs.zoomRatio
                                linearZoom = zs.linearZoom
                            }
                            cam.cameraInfo.zoomState.observe(lifecycleOwner) { zs ->
                                minZoomRatio = zs.minZoomRatio
                                maxZoomRatio = zs.maxZoomRatio
                                activeZoomRatio = zs.zoomRatio
                                linearZoom = zs.linearZoom
                            }
                            cam.cameraControl.setZoomRatio(1.0f.coerceIn(minZoomRatio, maxZoomRatio))
                        },
                        onCodeScanned = { raw, format ->
                            viewModel.onBarcodeDetected(raw, format)
                        },
                        onOcrRecognized = { fullText, lines ->
                            if (!ocrFrozen && fullText.isNotBlank()) {
                                liveOcrText = fullText
                                liveOcrLines = lines
                            }
                        },
                    )

                    // Precision Corner Reticle Overlay
                    val reticleColor = MaterialTheme.colorScheme.primary
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val boxW = size.width * (if (ocrMode) 0.84f else 0.72f)
                        val boxH = size.height * (if (ocrMode) 0.56f else 0.44f)
                        val left = (size.width - boxW) / 2f
                        val top = (size.height - boxH) / 2f
                        val right = left + boxW
                        val bottom = top + boxH
                        val corner = 24.dp.toPx()
                        val stroke = 3.2.dp.toPx()

                        drawLine(reticleColor, Offset(left, top), Offset(left + corner, top), stroke, StrokeCap.Round)
                        drawLine(reticleColor, Offset(left, top), Offset(left, top + corner), stroke, StrokeCap.Round)
                        drawLine(reticleColor, Offset(right, top), Offset(right - corner, top), stroke, StrokeCap.Round)
                        drawLine(reticleColor, Offset(right, top), Offset(right, top + corner), stroke, StrokeCap.Round)
                        drawLine(reticleColor, Offset(left, bottom), Offset(left + corner, bottom), stroke, StrokeCap.Round)
                        drawLine(reticleColor, Offset(left, bottom), Offset(left, bottom - corner), stroke, StrokeCap.Round)
                        drawLine(reticleColor, Offset(right, bottom), Offset(right - corner, bottom), stroke, StrokeCap.Round)
                        drawLine(reticleColor, Offset(right, bottom), Offset(right, bottom - corner), stroke, StrokeCap.Round)

                        if (!ocrMode) {
                            drawLine(
                                color = reticleColor.copy(alpha = 0.65f),
                                start = Offset(left + 12f, size.height / 2f),
                                end = Offset(right - 12f, size.height / 2f),
                                strokeWidth = 1.6.dp.toPx(),
                            )
                        }
                    }

                    if (deviceProfile.thermalLevel == ThermalHealthLevel.THROTTLED) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = StatusWarning.copy(alpha = 0.88f),
                        ) {
                            Text(
                                text = "Aquecimento elevado",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }

                    // Torch Toggle
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.55f),
                    ) {
                        IconButton(
                            onClick = {
                                torchEnabled = !torchEnabled
                                boundCamera?.cameraControl?.enableTorch(torchEnabled)
                            },
                            modifier = Modifier.testTag("scanner_torch_button"),
                        ) {
                            Icon(
                                imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Alternar lanterna",
                                tint = if (torchEnabled) MaterialTheme.colorScheme.primary else Color.White,
                            )
                        }
                    }

                    // Bottom Optical Lens Presets (0.6x Macro / 1x / 3x Telefoto) + Continuous Zoom Slider
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(10.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Black.copy(alpha = 0.68f),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            val lensPresets = buildList {
                                if (minZoomRatio <= 0.72f) add("0.6x Macro" to minZoomRatio.coerceAtLeast(0.6f))
                                add("1x Padrão" to 1.0f.coerceIn(minZoomRatio, maxZoomRatio))
                                if (maxZoomRatio >= 2.8f) add("3x Telefoto" to 3.0f.coerceIn(minZoomRatio, maxZoomRatio))
                            }
                            if (lensPresets.size > 1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    lensPresets.forEach { (label, targetRatio) ->
                                        val selected = kotlin.math.abs(activeZoomRatio - targetRatio) < 0.22f
                                        FilterChip(
                                            selected = selected,
                                            onClick = {
                                                boundCamera?.cameraControl?.setZoomRatio(targetRatio)
                                            },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                                            modifier = Modifier.weight(1f).testTag("scanner_lens_${label.take(3).trim()}"),
                                        )
                                    }
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "${"%.1f".format(activeZoomRatio)}x",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                )
                                Spacer(Modifier.width(8.dp))
                                Slider(
                                    value = linearZoom.coerceIn(0f, 1f),
                                    onValueChange = { z ->
                                        linearZoom = z
                                        boundCamera?.cameraControl?.setLinearZoom(z)
                                    },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Auto-Enter Toggle
        RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enviar Enter após digitar", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (ocrMode) "Reconhecimento 100% offline no celular via ML Kit"
                        else "Digita o código automaticamente no campo ativo do PC",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.scannerAutoEnter,
                    onCheckedChange = { checked -> viewModel.updateSettings { it.copy(scannerAutoEnter = checked) } },
                    modifier = Modifier.testTag("scanner_auto_enter_switch"),
                )
            }
        }

        // Bottom Panel: OCR Live Lines Panel OR Recent Barcode Scans History
        if (ocrMode) {
            RemoteSurface(
                modifier = Modifier.fillMaxWidth().weight(0.9f),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("TEXTO DETECTADO (TOQUE NUMA LINHA OU ENVIE TUDO)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { ocrFrozen = !ocrFrozen },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(if (ocrFrozen) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (ocrFrozen) "Retomar" else "Congelar")
                        }
                        Button(
                            onClick = { viewModel.sendOcrTextToPc(liveOcrText) },
                            enabled = liveOcrText.isNotBlank(),
                            modifier = Modifier.weight(1.3f).testTag("ocr_send_all_button"),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Digitar no PC")
                        }
                    }

                    if (liveOcrLines.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Aponte a câmera para um papel, senha Wi-Fi, boleto ou tela...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(liveOcrLines) { line ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { viewModel.sendOcrTextToPc(line) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outlineVariant),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = line,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Digitar linha", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            RemoteSurface(
                modifier = Modifier.fillMaxWidth().weight(0.85f),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("LEITURAS RECENTES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    if (scanHistory.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Aponte para um código de barras ou QR Code", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(scanHistory) { item ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { viewModel.resendScannedCode(item.value) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outlineVariant),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.value, fontFamily = JetBrainsMonoFontFamily, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(item.formatLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Reenviar código", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            boundCamera?.cameraControl?.enableTorch(false)
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraDualScannerPreview(
    ocrMode: Boolean,
    ocrPaused: Boolean,
    onCameraReady: (Camera) -> Unit,
    onCodeScanned: (String, String) -> Unit,
    onOcrRecognized: (String, List<String>) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    val currentOcrMode by rememberUpdatedState(ocrMode)
    val currentOcrPaused by rememberUpdatedState(ocrPaused)
    var boundProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { boundProvider?.unbindAll() }
            runCatching { barcodeScanner.close() }
            runCatching { textRecognizer.close() }
            analyzerExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()
                    boundProvider = cameraProvider
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    var lastOcrMs = 0L
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val inputImage = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees,
                                    )
                                    if (currentOcrMode) {
                                        val now = System.currentTimeMillis()
                                        if (currentOcrPaused || now - lastOcrMs < 450L) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }
                                        lastOcrMs = now
                                        textRecognizer.process(inputImage)
                                            .addOnSuccessListener { visionText ->
                                                val full = visionText.text.trim()
                                                val lines = visionText.textBlocks
                                                    .flatMap { it.lines }
                                                    .map { it.text.trim() }
                                                    .filter { it.isNotEmpty() }
                                                    .take(12)
                                                if (full.isNotEmpty()) {
                                                    onOcrRecognized(full, lines)
                                                }
                                            }
                                            .addOnCompleteListener { imageProxy.close() }
                                    } else {
                                        barcodeScanner.process(inputImage)
                                            .addOnSuccessListener { barcodes ->
                                                val first = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                                                if (first != null) {
                                                    val raw = first.rawValue.orEmpty()
                                                    val formatName = barcodeFormatLabel(first.format)
                                                    onCodeScanned(raw, formatName)
                                                }
                                            }
                                            .addOnCompleteListener { imageProxy.close() }
                                    }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis,
                    )
                    val factory = previewView.meteringPointFactory
                    val point = factory.createPoint(0.5f, 0.5f)
                    val action = FocusMeteringAction.Builder(point)
                        .setAutoCancelDuration(3, TimeUnit.SECONDS)
                        .build()
                    camera.cameraControl.startFocusAndMetering(action)
                    onCameraReady(camera)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}

private fun barcodeFormatLabel(format: Int): String = when (format) {
    Barcode.FORMAT_QR_CODE -> "QR Code"
    Barcode.FORMAT_EAN_13 -> "EAN-13"
    Barcode.FORMAT_EAN_8 -> "EAN-8"
    Barcode.FORMAT_CODE_128 -> "Code 128"
    Barcode.FORMAT_CODE_39 -> "Code 39"
    Barcode.FORMAT_UPC_A -> "UPC-A"
    Barcode.FORMAT_PDF417 -> "PDF417"
    Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
    else -> "Código de Barras"
}
