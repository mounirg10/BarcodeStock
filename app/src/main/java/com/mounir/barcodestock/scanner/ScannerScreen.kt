package com.mounir.barcodestock.scanner

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(onBarcode: (String) -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("مسح الباركود") },
            navigationIcon = { TextButton(onClick = onBack) { Text("رجوع") } }
        )
    }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            CameraScanner(
                modifier = Modifier.fillMaxSize(),
                continuous = false,
                onBarcode = onBarcode
            )
            Box(
                Modifier.align(Alignment.Center)
                    .size(300.dp, 170.dp)
                    .border(BorderStroke(3.dp, Color.White))
            )
            Text(
                "وجّه الكاميرا نحو الباركود",
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * عارض كاميرا قابل لإعادة الاستخدام (شاشة المسح وشاشة البيع).
 * في وضع البيع مرّر continuous = true لمسح عدة منتجات دون توقف.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScanner(
    modifier: Modifier = Modifier,
    continuous: Boolean = false,
    onBarcode: (String) -> Unit
) {
    val permission = rememberPermissionState(Manifest.permission.CAMERA)
    LaunchedEffect(Unit) { if (!permission.status.isGranted) permission.launchPermissionRequest() }

    if (!permission.status.isGranted) {
        Column(
            modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("التطبيق يحتاج إذن الكاميرا لقراءة الباركود", textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { permission.launchPermissionRequest() }) { Text("منح الإذن") }
        }
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val callback by rememberUpdatedState(onBarcode)

    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)

            providerFuture.addListener({
                val provider = providerFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also {
                        it.setAnalyzer(executor, BarcodeAnalyzer(continuous) { code -> callback(code) })
                    }

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                )
            }, androidx.core.content.ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}
