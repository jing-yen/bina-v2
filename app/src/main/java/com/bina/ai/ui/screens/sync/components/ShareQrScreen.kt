package com.bina.ai.ui.screens.sync.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.ui.screens.sync.SyncViewModel
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaPrimary
import com.bina.ai.ui.theme.BinaRed
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

@Composable
fun ShareQrScreen(
    vm: SyncViewModel,
    miniAppRepository: MiniAppRepository,
    recipeId: String,
    onDone: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val recipe = remember(recipeId) { miniAppRepository.getById(recipeId) }

    val encodeResult = remember(recipe) {
        recipe?.let { vm.encodeRecipeAsQr(it) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (recipe == null) {
            Text("Recipe not found", color = BinaGrayText)
        } else {
            Text(recipe.icon.ifBlank { "📦" }, fontSize = 36.sp)
            Text(recipe.name, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = BinaPrimary)
            recipe.author.name.takeIf { it.isNotBlank() }?.let {
                Text("by $it", fontSize = 12.sp, color = BinaGrayText)
            }
            Spacer(Modifier.height(8.dp))

            val payload = encodeResult?.getOrNull()
            val error = encodeResult?.exceptionOrNull()?.message

            if (payload != null) {
                val bitmap = remember(payload) {
                    val hints = mapOf(
                        com.google.zxing.EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L,
                        com.google.zxing.EncodeHintType.MARGIN to 2
                    )
                    BarcodeEncoder().encodeBitmap(payload, BarcodeFormat.QR_CODE, 2048, 2048, hints)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR for ${recipe.name}", modifier = Modifier.fillMaxSize())
                }
                Text(
                    "Have the other phone open Sync → Scan to Receive.\nSize: ${payload.length} chars",
                    fontSize = 12.sp, color = BinaGrayText
                )
            } else if (error != null) {
                Text(error, fontSize = 12.sp, color = BinaRed)
                Button(onClick = {
                    val maybeYaml = miniAppRepository.getYamlById(recipe.id)
                    if (maybeYaml != null) clipboard.setText(AnnotatedString(maybeYaml))
                }, colors = ButtonDefaults.buttonColors(containerColor = BinaPrimary)) {
                    Text("Copy YAML to clipboard")
                }
            }

            Spacer(Modifier.weight(1f))
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = BinaPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done")
            }
        }
    }
}
