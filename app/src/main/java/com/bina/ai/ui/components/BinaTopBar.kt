package com.bina.ai.ui.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    "in" to "🇮🇩",
    "vi" to "🇻🇳",
    "th" to "🇹🇭",
    "km" to "🇰🇭",
    "my" to "🇲🇲",
    "ta" to "🇮🇳",
    "zh" to "🇨🇳",
)

private val LANG_NAMES = mapOf(
    "ms" to "Bahasa Melayu",
    "en" to "English",
    "in" to "Bahasa Indonesia",
    "vi" to "Tiếng Việt",
    "th" to "ภาษาไทย",
    "km" to "ភាសាខ្មែរ",
    "my" to "မြန်မာဘာသာ",
    "ta" to "தமிழ்",
    "zh" to "中文",
)

@Composable
fun BinaTopBar(modifier: Modifier = Modifier) {
    val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { "ms" }
    val appLang = APP_LANGS.map { it.first }.firstOrNull { currentLocale.startsWith(it) } ?: "ms"
    val currentFlag = APP_LANGS.firstOrNull { it.first == appLang }?.second ?: "🇲🇾"
    var expanded by remember { mutableStateOf(false) }

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

        Box {
            Text(
                currentFlag,
                fontSize = 22.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BinaAccent.copy(alpha = 0.12f))
                    .clickable { expanded = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                APP_LANGS.forEach { (code, flag) ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(flag, fontSize = 18.sp)
                                Text(
                                    LANG_NAMES[code] ?: code,
                                    fontSize = 14.sp,
                                    fontWeight = if (code == appLang) FontWeight.Bold else FontWeight.Normal,
                                    color = if (code == appLang) BinaAccent else BinaStone950
                                )
                            }
                        },
                        onClick = {
                            expanded = false
                            AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags(code)
                            )
                        }
                    )
                }
            }
        }
    }
}
