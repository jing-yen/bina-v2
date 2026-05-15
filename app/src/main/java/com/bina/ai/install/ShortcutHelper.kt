package com.bina.ai.install

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.bina.ai.MainActivity

object ShortcutHelper {

    const val EXTRA_RECIPE_ID = "bina_shortcut_recipe_id"

    /**
     * Requests a pinned home-screen shortcut for the given recipe.
     * The shortcut icon is the recipe emoji rendered onto a bitmap.
     */
    fun pinRecipeToHomeScreen(
        context: Context,
        recipeId: String,
        recipeName: String,
        emoji: String
    ) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return

        val icon = createEmojiIcon(emoji)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_RECIPE_ID, recipeId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val shortcutInfo = ShortcutInfoCompat.Builder(context, "recipe_$recipeId")
            .setShortLabel(recipeName)
            .setLongLabel(recipeName)
            .setIcon(icon)
            .setIntent(launchIntent)
            .build()

        ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
    }

    private fun createEmojiIcon(emoji: String): IconCompat {
        val size = 192 // adaptive icon safe zone
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // White circular background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)

        // Draw the emoji centred
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size * 0.55f
            textAlign = Paint.Align.CENTER
        }
        val metrics = textPaint.fontMetrics
        val yOffset = (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(emoji, size / 2f, size / 2f - yOffset, textPaint)

        return IconCompat.createWithAdaptiveBitmap(bitmap)
    }
}
