package com.bina.ai.ui.screens.sync.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.R
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaStone950
import com.bina.ai.ui.theme.BinaBgCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareRecipePickerSheet(
    recipes: List<MiniApp>,
    onPick: (MiniApp) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = BinaBgCard) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.sync_share_title), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BinaStone950)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.sync_share_picker_subtitle),
                fontSize = 12.sp, color = BinaGrayText
            )
            Spacer(Modifier.height(16.dp))
            if (recipes.isEmpty()) {
                Text(
                    stringResource(R.string.sync_share_empty),
                    fontSize = 13.sp, color = BinaGrayText
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recipes.forEach { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF5F0EB))
                                .clickable { onPick(r) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(r.icon.ifBlank { "📦" }, fontSize = 24.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(r.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BinaStone950)
                                Text(r.category.ifBlank { "—" }, fontSize = 11.sp, color = BinaGrayText)
                            }
                        }
                    }
                }
            }
        }
    }
}
