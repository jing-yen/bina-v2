package com.bina.ai.ui.screens.sync.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasteYamlSheet(
    onImport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current
    var text by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Paste recipe YAML", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BinaPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Paste the YAML you copied from Studio (or another phone). It'll be parsed and previewed before install.",
                fontSize = 12.sp, color = BinaGrayText
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 320.dp),
                placeholder = { Text("id: my_recipe\nname: ...") }
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    text = clipboard.getText()?.text.orEmpty()
                }) { Text("Paste from clipboard") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { onImport(text) },
                    enabled = text.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = BinaPrimary)
                ) { Text("Import") }
            }
        }
    }
}
