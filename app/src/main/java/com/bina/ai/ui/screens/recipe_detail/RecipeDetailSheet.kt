package com.bina.ai.ui.screens.recipe_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddToHomeScreen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.R
import com.bina.ai.install.ShortcutHelper
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.screens.hub.components.RecipeCover
import com.bina.ai.ui.theme.BinaAccent
import com.bina.ai.ui.theme.BinaBgCard
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaIndigo
import com.bina.ai.ui.theme.BinaRed
import com.bina.ai.ui.theme.BinaStone950

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailSheet(
    miniApp: MiniApp,
    isInstalled: Boolean,
    sizeKb: Float,
    onConfigureInstall: () -> Unit,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BinaBgCard
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            // Hero strip
            Box(modifier = Modifier.fillMaxWidth().height(180.dp).padding(horizontal = 16.dp)) {
                RecipeCover(
                    miniApp,
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    cornerRadius = 18.dp,
                    emojiFontSize = 56
                )
                if (miniApp.emergency) {
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BinaRed)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("EMERGENCY", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 16.dp)) {
                Text(miniApp.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BinaStone950)
                if (miniApp.author.verified && miniApp.author.organisation.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "✓ Verified by ${miniApp.author.organisation}",
                        fontSize = 12.sp,
                        color = BinaGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (miniApp.description.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(miniApp.description, fontSize = 13.sp, color = BinaGrayText)
                }

                Spacer(Modifier.height(16.dp))
                RecipeStats(
                    sizeKb = sizeKb,
                    availableFeatures = miniApp.features.size,
                    dialect = miniApp.dialect
                )

                if (miniApp.tags.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(miniApp.tags) { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BinaIndigo.copy(alpha = 0.08f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("#$tag", fontSize = 11.sp, color = BinaIndigo)
                            }
                        }
                    }
                }

                if (miniApp.category.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Domain: ${miniApp.category}", fontSize = 12.sp, color = BinaGrayText)
                }

                if (miniApp.features.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Features in this recipe",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BinaGrayText
                    )
                    Spacer(Modifier.height(6.dp))
                    FeaturePreviewList(miniApp.features)
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = if (isInstalled) onOpen else onConfigureInstall,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BinaAccent)
                ) {
                    Text(
                        when {
                            isInstalled -> stringResource(R.string.recipe_open)
                            miniApp.features.isEmpty() -> stringResource(R.string.recipe_install_open)
                            else -> stringResource(R.string.recipe_configure_install)
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (isInstalled) {
                    val context = LocalContext.current
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            ShortcutHelper.pinRecipeToHomeScreen(
                                context = context,
                                recipeId = miniApp.id,
                                recipeName = miniApp.name,
                                emoji = miniApp.icon
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BinaAccent)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.AddToHomeScreen,
                                contentDescription = null,
                                tint = BinaAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.recipe_add_home),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = BinaStone950
                            )
                        }
                    }
                }
            }
        }
    }
}
