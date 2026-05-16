package com.bina.ai.ui.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.core.os.LocaleListCompat
import com.bina.ai.R
import com.bina.ai.ui.theme.BinaAccent
import com.bina.ai.ui.theme.BinaBgCard
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaStone950

private val APP_LANGS = listOf(
    "ms" to "🇲🇾",
    "en" to "🇬🇧",
)

@Composable
fun BinaTopBar(modifier: Modifier = Modifier) {
    val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { "ms" }
    val appLang = if (currentLocale.startsWith("ms")) "ms" else if (currentLocale.startsWith("en")) "en" else "ms"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BinaBgCard.copy(alpha = 0.85f))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_bina_mascot),
            contentDescription = "Bina",
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            APP_LANGS.forEach { (code, flag) ->
                val active = code == appLang
                Text(
                    flag,
                    fontSize = if (active) 16.sp else 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .then(
                            if (active) Modifier.background(BinaAccent.copy(alpha = 0.12f))
                            else Modifier
                        )
                        .clickable {
                            AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags(code)
                            )
                        }
                        .padding(horizontal = 3.dp, vertical = 2.dp)
                )
            }
        }
    }
}
