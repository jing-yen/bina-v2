package com.bina.ai.ui.screens.hub.components

import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource
import com.bina.ai.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.theme.BinaRed
import kotlinx.coroutines.delay

@Composable
fun FeaturedCarousel(
    recipes: List<MiniApp>,
    onRecipeClick: (MiniApp) -> Unit,
    modifier: Modifier = Modifier
) {
    if (recipes.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { recipes.size })

    LaunchedEffect(recipes.size) {
        if (recipes.size <= 1) return@LaunchedEffect
        while (true) {
            delay(6000)
            val next = (pagerState.currentPage + 1) % recipes.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 0.dp),
            pageSpacing = 10.dp,
            modifier = Modifier.fillMaxWidth().height(150.dp)
        ) { page ->
            FeaturedCard(recipes[page], onClick = { onRecipeClick(recipes[page]) })
        }
        if (recipes.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(recipes.size) { i ->
                    val active = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(if (active) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (active) Color(0xFFC45A3A) else Color(0xFFE7E0D8))
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedCard(miniApp: MiniApp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        RecipeCover(miniApp, modifier = Modifier.fillMaxSize(), cornerRadius = 16.dp, emojiFontSize = 48)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            if (miniApp.emergency) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BinaRed)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(stringResource(R.string.emergency_badge), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(com.bina.ai.ui.localizedName(miniApp), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Icon(Icons.Filled.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
            if (miniApp.description.isNotBlank()) {
                Text(
                    com.bina.ai.ui.localizedDescription(miniApp),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
