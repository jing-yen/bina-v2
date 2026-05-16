package com.bina.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.ui.navigation.Screen
import com.bina.ai.ui.theme.BinaAccent
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaNavActive
import com.bina.ai.ui.theme.BinaNavSurface

@Composable
fun BinaBottomNav(
    currentRoute: String?,
    onTabClick: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = Screen.tabs()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(0.dp))
            .background(BinaNavSurface)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isActive = currentRoute == tab.route

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabClick(tab) }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (isActive) Modifier.background(BinaNavActive)
                                else Modifier
                            )
                            .padding(horizontal = 14.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(18.dp),
                            tint = if (isActive) BinaAccent else BinaGrayText
                        )
                    }
                    Text(
                        text = if (tab.labelRes != 0) stringResource(tab.labelRes) else tab.label,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isActive) BinaAccent else BinaGrayText
                    )
                }
            }
        }
    }
}
