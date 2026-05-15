package com.bina.ai.analytics.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.analytics.ui.model.DailyBucket
import com.bina.ai.analytics.ui.model.MetricsSnapshot
import com.bina.ai.analytics.ui.model.RecipeStats
import com.bina.ai.analytics.ui.util.plural
import com.bina.ai.ui.theme.BinaBgCard
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaStone950
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricDetailSheet(
    kind: MetricKind,
    metrics: MetricsSnapshot,
    leaderboard: List<RecipeStats>,
    chart: List<DailyBucket>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BinaBgCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            when (kind) {
                MetricKind.INSTALLED -> InstalledRecipesDetail(metrics, leaderboard)
                MetricKind.QUESTIONS -> QuestionsDetail(metrics, leaderboard)
                MetricKind.ACTIVE_DAYS -> ActiveDaysDetail(metrics, chart)
                MetricKind.STREAK -> StreakDetail(metrics, chart)
            }
        }
    }
}

// ------- per-kind sections ------------------------------------------------

@Composable
private fun InstalledRecipesDetail(metrics: MetricsSnapshot, leaderboard: List<RecipeStats>) {
    val installed = metrics.recipesInstalled
    SheetHeader(
        title = "Recipes Installed",
        subtitle = "$installed ${plural(installed, "recipe", "recipes")} in your Pocket"
    )
    val used = leaderboard.filter { it.total > 0 }
    if (installed == 0) {
        SheetHint("Browse the Hub to install recipes. They'll appear in MyPocket and here.")
    } else if (used.isEmpty()) {
        SheetHint("You haven't used any installed recipes in this period yet. Open one from MyPocket.")
    } else {
        Text("Most-used in this period", fontSize = 11.sp, color = BinaGrayText, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            used.forEach { row ->
                ListLine(
                    leading = row.icon,
                    primary = row.displayName,
                    secondary = "${row.launches} ${plural(row.launches, "launch", "launches")} · " +
                        "${row.asks} ${plural(row.asks, "ask", "asks")}"
                )
            }
        }
    }
}

@Composable
private fun QuestionsDetail(metrics: MetricsSnapshot, leaderboard: List<RecipeStats>) {
    val total = metrics.questionsAsked
    SheetHeader(
        title = "Questions Asked",
        subtitle = "$total ${plural(total, "question", "questions")} in this period"
    )
    val byAsks = leaderboard.filter { it.asks > 0 }.sortedByDescending { it.asks }
    if (byAsks.isEmpty()) {
        SheetHint("Ask a recipe a question and it'll show up here, broken down by recipe.")
    } else {
        Text("By recipe", fontSize = 11.sp, color = BinaGrayText, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            byAsks.forEach { row ->
                ListLine(
                    leading = row.icon,
                    primary = row.displayName,
                    secondary = "${row.asks} ${plural(row.asks, "ask", "asks")}"
                )
            }
        }
    }
}

@Composable
private fun ActiveDaysDetail(metrics: MetricsSnapshot, chart: List<DailyBucket>) {
    val active = chart.filter { it.total > 0 }.sortedByDescending { it.dayStartMs }
    SheetHeader(
        title = "Active Days",
        subtitle = "${metrics.activeDays} of ${chart.size} ${plural(chart.size, "day", "days")} with activity"
    )
    if (active.isEmpty()) {
        SheetHint("Days where you launch a recipe or ask a question count toward your streak.")
    } else {
        val df = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            active.forEach { b ->
                ListLine(
                    leading = "📅",
                    primary = df.format(Date(b.dayStartMs)),
                    secondary = "${b.launches} ${plural(b.launches, "launch", "launches")} · " +
                        "${b.asks} ${plural(b.asks, "ask", "asks")}"
                )
            }
        }
    }
}

@Composable
private fun StreakDetail(metrics: MetricsSnapshot, chart: List<DailyBucket>) {
    val streak = metrics.currentStreak
    val subtitle = when (streak) {
        0 -> "No active streak right now"
        1 -> "1 day in a row"
        else -> "$streak days in a row"
    }
    SheetHeader(title = "Streak", subtitle = subtitle)
    val recent = chart.filter { it.total > 0 }.sortedByDescending { it.dayStartMs }.take(7)
    if (recent.isEmpty()) {
        SheetHint("Open or ask a recipe today to start a streak. Consecutive active days build it up.")
    } else {
        val df = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        Text("Recent active days", fontSize = 11.sp, color = BinaGrayText, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            recent.forEach { b ->
                ListLine(
                    leading = "🔥",
                    primary = df.format(Date(b.dayStartMs)),
                    secondary = "${b.launches} ${plural(b.launches, "launch", "launches")} · " +
                        "${b.asks} ${plural(b.asks, "ask", "asks")}"
                )
            }
        }
    }
}

// ------- shared bits ------------------------------------------------------

@Composable
private fun SheetHeader(title: String, subtitle: String) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BinaStone950)
    Spacer(Modifier.height(4.dp))
    Text(subtitle, fontSize = 13.sp, color = BinaGrayText)
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun SheetHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F0EB))
            .padding(12.dp)
    ) {
        Text(text, fontSize = 12.sp, color = BinaGrayText)
    }
}

@Composable
private fun ListLine(leading: String, primary: String, secondary: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF5F0EB))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BinaBgCard),
            contentAlignment = Alignment.Center
        ) {
            Text(leading, fontSize = 16.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BinaStone950)
            Text(secondary, fontSize = 11.sp, color = BinaGrayText)
        }
    }
}
