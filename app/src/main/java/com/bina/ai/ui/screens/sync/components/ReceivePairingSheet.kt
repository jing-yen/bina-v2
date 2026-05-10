package com.bina.ai.ui.screens.sync.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.sync.BlePairingPayload
import com.bina.ai.ui.screens.sync.TransferState
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaPrimary
import com.bina.ai.ui.theme.BinaRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivePairingSheet(
    offer: BlePairingPayload.Offer,
    transferState: TransferState,
    onConnect: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onCancel, sheetState = sheetState, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Receive Recipe?", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BinaPrimary)
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF9FAFB))
                    .padding(14.dp)
            ) {
                Column {
                    Text(offer.recipeName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = BinaPrimary)
                    Spacer(Modifier.height(2.dp))
                    if (offer.authorName.isNotBlank()) {
                        Text("by ${offer.authorName}", fontSize = 12.sp, color = BinaGrayText)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("${offer.sizeBytes / 1024} KB · id: ${offer.recipeId}", fontSize = 11.sp, color = BinaGrayText)
                }
            }
            Spacer(Modifier.height(16.dp))

            when (transferState) {
                is TransferState.Idle -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onCancel) { Text("Cancel") }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = onConnect,
                            colors = ButtonDefaults.buttonColors(containerColor = BinaPrimary)
                        ) { Text("Connect") }
                    }
                }
                is TransferState.Connecting -> {
                    Text("Connecting to sender…", fontSize = 13.sp, color = BinaGrayText)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is TransferState.InProgress -> {
                    Text("Receiving… ${transferState.pct}%", fontSize = 13.sp, color = BinaGrayText)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { transferState.pct / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is TransferState.Failed -> {
                    Text(transferState.message, fontSize = 13.sp, color = BinaRed)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onCancel) { Text("Cancel") }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = BinaPrimary)
                        ) { Text("Retry") }
                    }
                }
            }
        }
    }
}
