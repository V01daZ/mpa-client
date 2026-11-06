package dev.mpa.client.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dev.mpa.client.ui.theme.Ground
import dev.mpa.client.ui.theme.OnBoard
import dev.mpa.client.ui.theme.OnBoardMuted
import dev.mpa.client.ui.theme.Paper
import dev.mpa.client.ui.theme.PaperInk
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    onResult: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var asked by remember { mutableStateOf(false) }
    var scanned by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        asked = true
    }
    LaunchedEffect(Unit) { if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    Scaffold(
        containerColor = Ground,
        topBar = {
            TopAppBar(
                title = { Text("QR-код") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Ground, titleContentColor = OnBoard, navigationIconContentColor = OnBoard,
                ),
            )
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            if (hasCameraPermission) {
                val executor = remember { Executors.newSingleThreadExecutor() }
                val scanner = remember { BarcodeScanning.getClient() }
                var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
                // Камера привязана к жизненному циклу Activity — без unbind она оставалась включённой после выхода
                DisposableEffect(Unit) {
                    onDispose {
                        provider?.unbindAll()
                        scanner.close()
                        executor.shutdown()
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val future = ProcessCameraProvider.getInstance(ctx)
                        future.addListener({
                            val cameraProvider = future.get().also { provider = it }
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            analysis.setAnalyzer(executor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (scanned || mediaImage == null) { imageProxy.close(); return@setAnalyzer }
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(image)
                                    .addOnSuccessListener { codes ->
                                        codes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }?.rawValue?.let { value ->
                                            if (!scanned) { scanned = true; onResult(value) }
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            }
                            runCatching {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.7f)
                            .aspectRatio(1f)
                            .border(3.dp, Paper, MaterialTheme.shapes.large)
                    )
                }
                Text(
                    "Наведи камеру на QR-код от бота",
                    color = OnBoard,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(24.dp)
                        .background(Ground.copy(alpha = 0.75f), MaterialTheme.shapes.medium)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Нужен доступ к камере", color = OnBoard, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Без него QR-код не прочитать. Ключ можно и вставить вручную.",
                        color = OnBoardMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (!asked) permissionLauncher.launch(Manifest.permission.CAMERA)
                            else context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Paper, contentColor = PaperInk),
                    ) { Text(if (asked) "Открыть настройки" else "Разрешить") }
                }
            }
        }
    }
}
