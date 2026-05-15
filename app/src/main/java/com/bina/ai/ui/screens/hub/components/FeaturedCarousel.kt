package com.bina.ai.ui.screens.hub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
            delay(4000)
            val next = (pagerState.currentPage + 1) % recipes.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 0.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth().height(200.dp)
        ) { page ->
            FeaturedCard(recipes[page], onClick = { onRecipeClick(recipes[page]) })
        }
        if (recipes.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(recipes.size) { i ->
                    val active = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (active) 8.dp else 6.dp)
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
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        RecipeCover(miniApp, modifier = Modifier.fillMaxSize(), cornerRadius = 20.dp, emojiFontSize = 72)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            if (miniApp.emergency) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BinaRed)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("EMERGENCY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Text(miniApp.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (miniApp.description.isNotBlank()) {
                Text(
                    miniApp.description,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2
                )
            }
        }
    }
}
