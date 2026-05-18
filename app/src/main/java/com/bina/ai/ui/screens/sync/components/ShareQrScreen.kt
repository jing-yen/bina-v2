package com.bina.ai.ui.screens.sync.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.R
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.ui.screens.sync.SyncViewModel
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaStone950
import com.bina.ai.ui.theme.BinaAccent
import com.bina.ai.ui.theme.BinaRed
import com.bina.ai.ui.theme.BinaBgCard
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

    val context = LocalContext.current
    val yamlText = remember(recipe) {
        recipe?.let { miniAppRepository.getYamlById(it.id) }
    }
    val sender = remember(recipe, yamlText) {
        if (recipe == null || yamlText == null) null
        else com.bina.ai.sync.BleSender(
            context = context,
            serviceUuid = java.util.UUID.randomUUID(),
            payloadBytes = yamlText.toByteArray(Charsets.UTF_8)
        )
    }
    val pairingPayload = remember(recipe, sender, yamlText) {
        if (recipe == null || sender == null || yamlText == null) null
        else com.bina.ai.sync.BlePairingPayload.encode(
            com.bina.ai.sync.BlePairingPayload.Offer(
                serviceUuid = sender.serviceUuid,
                recipeId = recipe.id,
                sizeBytes = yamlText.toByteArray(Charsets.UTF_8).size.toLong(),
                recipeName = recipe.name,
                authorName = recipe.author.name
            )
        )
    }

    LaunchedEffect(sender) {
        if (sender != null && com.bina.ai.sync.BlePermissions.hasSenderPermissions(context)) {
            sender.start()
        }
    }

    val blePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it } && sender != null) {
            sender.start()
        }
    }
    LaunchedEffect(Unit) {
        if (!com.bina.ai.sync.BlePermissions.hasSenderPermissions(context)) {
            blePermLauncher.launch(com.bina.ai.sync.BlePermissions.SENDER_PERMISSIONS)
        }
    }

    DisposableEffect(sender) {
        onDispose { sender?.stop() }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (recipe == null) {
            Text(stringResource(R.string.sync_recipe_not_found), color = BinaGrayText)
        } else {
            Text(recipe.icon.ifBlank { "📦" }, fontSize = 36.sp)
            Text(recipe.name, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = BinaStone950)
            recipe.author.name.takeIf { it.isNotBlank() }?.let {
                Text(stringResource(R.string.sync_receive_by, it), fontSize = 12.sp, color = BinaGrayText)
            }
            Spacer(Modifier.height(8.dp))

            if (pairingPayload != null) {
                val bitmap = remember(pairingPayload) {
                    val hints = mapOf(
                        com.google.zxing.EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L,
                        com.google.zxing.EncodeHintType.MARGIN to 2
                    )
                    BarcodeEncoder().encodeBitmap(pairingPayload, BarcodeFormat.QR_CODE, 2048, 2048, hints)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(BinaBgCard)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Pairing QR for ${recipe.name}", modifier = Modifier.fillMaxSize())
                }
                Text(
                    stringResource(R.string.sync_share_instructions),
                    fontSize = 12.sp, color = BinaGrayText
                )
            } else {
                Text(
                    stringResource(R.string.sync_bt_needed),
                    fontSize = 12.sp, color = BinaRed
                )
                Button(onClick = {
                    if (yamlText != null) clipboard.setText(AnnotatedString(yamlText))
                }, colors = ButtonDefaults.buttonColors(containerColor = BinaAccent)) {
                    Text(stringResource(R.string.sync_copy_yaml))
                }
            }

            Spacer(Modifier.weight(1f))
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = BinaAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.sync_done))
            }
        }
    }
}
