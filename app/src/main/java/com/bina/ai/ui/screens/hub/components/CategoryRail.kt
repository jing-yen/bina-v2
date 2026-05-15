package com.bina.ai.ui.screens.hub.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.theme.BinaStone950

@Composable
fun CategoryRail(
    title: String,
    recipes: List<MiniApp>,
    installedIds: Set<String>,
    onRecipeClick: (MiniApp) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = BinaStone950
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(recipes, key = { it.id }) { recipe ->
                RecipeCard(
                    miniApp = recipe,
                    isInstalled = recipe.id in installedIds,
                    onClick = { onRecipeClick(recipe) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
