package com.bina.ai.ui.screens.sync.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bina.ai.ui.screens.sync.IncomingState
import com.bina.ai.ui.screens.sync.SyncViewModel
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaPrimary
import com.bina.ai.ui.theme.BinaRed
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompoundBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory

@Composable
fun ScanQrScreen(
    vm: SyncViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val incoming by vm.incoming.collectAsStateWithLifecycle()
    val pairing by vm.pairing.collectAsStateWithLifecycle()
    val transferState by vm.transfer.collectAsStateWithLifecycle()
    var receiver by remember { mutableStateOf<com.bina.ai.sync.BleReceiver?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun startTransfer(offer: com.bina.ai.sync.BlePairingPayload.Offer) {
        if (!com.bina.ai.sync.BlePermissions.hasReceiverPermissions(context)) {
            vm.onTransferFailed("Bluetooth permission required")
            return
        }
        vm.onTransferConnecting()
        val r = com.bina.ai.sync.BleReceiver(context, offer.serviceUuid, offer.sizeBytes)
        receiver = r
        coroutineScope.launch {
            r.state.collect { s ->
                when (s) {
                    is com.bina.ai.sync.ReceiverState.Done -> {
                        vm.onTransferComplete(s.payload)
                        r.stop()
                        receiver = null
                    }
                    is com.bina.ai.sync.ReceiverState.Failed -> {
                        vm.onTransferFailed(s.message)
                    }
                    is com.bina.ai.sync.ReceiverState.Receiving -> {
                        vm.onTransferProgress(s.pct)
                    }
                    else -> { /* Scanning, Connecting, Idle — Connecting state is set above */ }
                }
            }
        }
        r.start()
    }

    DisposableEffect(Unit) {
        onDispose { receiver?.stop() }
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    val blePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* if any denied, the receiver flow will surface its own error */ }
    LaunchedEffect(Unit) {
        if (!com.bina.ai.sync.BlePermissions.hasReceiverPermissions(context)) {
            blePermLauncher.launch(com.bina.ai.sync.BlePermissions.RECEIVER_PERMISSIONS)
        }
    }

    var showPaste by remember { mutableStateOf(false) }

    // Once VM is in Ready or just transitioned via an install, navigate up to Configurator.
    LaunchedEffect(incoming) {
        when (val s = incoming) {
            is IncomingState.Ready -> {
                // Defer to caller — receive preview RecipeDetailSheet handled in OfflineSyncScreen.
                // We need to bounce back so the parent shows the sheet.
                // Approach: stash the state, pop back to OfflineSync, and let OfflineSync's
                // collected `incoming` flow render the preview sheet.
                onBack()
            }
            is IncomingState.Error -> { /* surfaced inline below */ }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            !hasPermission -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Camera permission needed", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Allow camera to scan a QR, or paste a YAML instead.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { launcher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = BinaPrimary)
                    ) { Text("Try again") }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Paste YAML instead",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { showPaste = true }
                    )
                }
            }
            else -> {
                val barcodeViewRef = remember { mutableStateOf<CompoundBarcodeView?>(null) }
                AndroidView(
                    factory = { ctx ->
                        CompoundBarcodeView(ctx).also { bv ->
                            barcodeViewRef.value = bv
                            // Lock decoder to QR_CODE only — faster + more reliable than
                            // the default multi-format scan.
                            bv.barcodeView.decoderFactory =
                                DefaultDecoderFactory(listOf(com.google.zxing.BarcodeFormat.QR_CODE))
                            // Default framing rect is a small square in the middle —
                            // dense QRs often don't fit. Set a generous size so the
                            // whole camera frame is decoded.
                            bv.barcodeView.setFramingRectSize(com.journeyapps.barcodescanner.Size(1600, 1600))
                            // Tune the camera HAL for barcode scanning: continuous
                            // autofocus + barcode scene mode = much better focus on
                            // close-up dense QR codes. Without these, the default
                            // single-shot autofocus often gives up on dense codes.
                            bv.barcodeView.cameraSettings = bv.barcodeView.cameraSettings.apply {
                                isAutoFocusEnabled = true
                                isContinuousFocusEnabled = true
                                isBarcodeSceneModeEnabled = true
                                isMeteringEnabled = true
                            }
                            bv.decodeContinuous(object : BarcodeCallback {
                                override fun barcodeResult(result: BarcodeResult) {
                                    bv.pause()
                                    vm.handleScannedQr(result.text)
                                }
                                override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>) {}
                            })
                            bv.resume()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                DisposableEffect(Unit) {
                    onDispose { barcodeViewRef.value?.pause() }
                }
                // Bottom strip
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 32.dp), contentAlignment = Alignment.BottomCenter) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        (incoming as? IncomingState.Error)?.let {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BinaRed.copy(alpha = 0.85f))
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(it.message, color = Color.White, fontSize = 12.sp)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.92f))
                                .clickable { showPaste = true }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text("Paste YAML instead", color = BinaPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    pairing?.let { offer ->
        ReceivePairingSheet(
            offer = offer,
            transferState = transferState,
            onConnect = { startTransfer(offer) },
            onCancel = {
                receiver?.stop()
                receiver = null
                vm.dismissPairing()
            },
            onRetry = { startTransfer(offer) }
        )
    }

    if (showPaste) {
        PasteYamlSheet(
            onImport = { yaml ->
                showPaste = false
                vm.handlePastedYaml(yaml)
            },
            onDismiss = { showPaste = false }
        )
    }
}
