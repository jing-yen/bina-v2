package com.bina.ai.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.R
import com.bina.ai.ui.theme.BinaBgCard
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaStone950

@Composable
fun BinaTopBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BinaBgCard.copy(alpha = 0.85f))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Image(
            painter = painterResource(R.drawable.ic_bina_mascot),
            contentDescription = "Bina",
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(Modifier.width(8.dp))

        Column {
            Text(
                stringResource(R.string.topbar_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = BinaStone950
            )
            Text(
                stringResource(R.string.topbar_subtitle),
                fontSize = 10.sp,
                color = BinaGrayText
            )
        }
    }
}
