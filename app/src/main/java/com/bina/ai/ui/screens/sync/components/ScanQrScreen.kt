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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.sync.RecipeImporter
import com.bina.ai.ui.screens.sync.IncomingState
import com.bina.ai.ui.screens.sync.SyncViewModel
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaPrimary
import com.bina.ai.ui.theme.BinaRed
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompoundBarcodeView

@Composable
fun ScanQrScreen(
    miniAppRepository: MiniAppRepository,
    installStore: InstallStore,
    onImported: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val factory = remember(miniAppRepository, installStore) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val importer = RecipeImporter(filesDir = context.filesDir, miniAppRepository = miniAppRepository)
                return SyncViewModel(miniAppRepository, installStore, importer) as T
            }
        }
    }
    val vm: SyncViewModel = viewModel(factory = factory)
    val incoming by vm.incoming.collectAsStateWithLifecycle()

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
                AndroidView(
                    factory = { ctx ->
                        CompoundBarcodeView(ctx).apply {
                            decodeContinuous(object : BarcodeCallback {
                                override fun barcodeResult(result: BarcodeResult) {
                                    pause()
                                    vm.handleScannedQr(result.text)
                                }
                                override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>) {}
                            })
                            resume()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
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
