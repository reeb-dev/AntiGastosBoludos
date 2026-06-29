package com.antigastos.boludos.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.antigastos.boludos.domain.CopyMood
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.ui.theme.ArgPalette
import java.io.File
import java.io.FileOutputStream

/**
 * Renderiza la frase de personalidad a un Bitmap (1080x1080) y lanza el ShareSheet.
 * No depende de capturar Composables: dibuja con Canvas para no perder calidad.
 */
object ShareCardRenderer {

    fun shareCopy(context: Context, message: String, mood: CopyMood) {
        val bitmap = renderToBitmap(message, mood)
        shareBitmap(context, bitmap, caption = "$message — Anti-gastos boludos", chooserTitle = "Compartir reacción")
    }

    /** Tarjeta tipo “Wrapped” cuadrada (1080x1080) con varias líneas de texto. */
    fun shareMonthlyRecap(context: Context, title: String, lines: List<String>, mood: CopyMood) {
        val msg = buildString {
            appendLine(title)
            lines.forEach { appendLine("• $it") }
        }
        shareCopy(context, msg.trim(), mood)
    }

    /**
     * Versión "story" 9:16 del recap mensual: total + persona + top líneas
     * de cierre. Pensada para ig stories / status. Reusa el mismo motor de
     * gradientes que la daily card.
     */
    fun shareMonthlyRecapStory(
        context: Context,
        monthLabel: String,
        totalPesos: Long,
        personaEmoji: String,
        personaName: String,
        highlights: List<String>,
        closingQuote: String,
        mood: CopyMood,
    ) {
        val bitmap = renderMonthlyRecapStoryBitmap(
            monthLabel = monthLabel,
            totalPesos = totalPesos,
            personaEmoji = personaEmoji,
            personaName = personaName,
            highlights = highlights,
            closingQuote = closingQuote,
            mood = mood,
        )
        val caption = buildString {
            appendLine("Mi recap de $monthLabel en Anti-gastos boludos 🇦🇷")
            appendLine("Total: ${MoneyFormat.formatPesos(totalPesos)}")
            highlights.take(3).forEach { appendLine("• $it") }
            appendLine()
            append(closingQuote)
        }
        shareBitmap(context, bitmap, caption = caption.trim(), chooserTitle = "Compartir mi mes")
    }

    /**
     * Historia vertical 9:16: total del día + personaje + frase (Home / viral).
     */
    fun shareDailyCard(
        context: Context,
        daySpentPesos: Long,
        personaEmoji: String,
        personaName: String,
        quote: String,
        mood: CopyMood,
    ) {
        val lucas = MoneyFormat.pesosToLucas(daySpentPesos)
        val sub = "Hoy gastaste $lucas lucas · $personaName"
        val bitmap = renderDailyStoryBitmap(sub, personaEmoji, quote, mood)
        val caption = buildString {
            appendLine("Mi día en Anti-gastos boludos 🇦🇷")
            appendLine(sub)
            appendLine()
            append(quote)
        }
        shareBitmap(context, bitmap, caption = caption.trim(), chooserTitle = "Compartir mi día")
    }

    private fun shareBitmap(context: Context, bitmap: Bitmap, caption: String, chooserTitle: String) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "antigastos_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { os ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, os)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, chooserTitle))
    }

    private fun renderToBitmap(message: String, mood: CopyMood): Bitmap {
        val size = 1080
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val (top, bottom, accent, emoji) = palette(mood)
        val grad = LinearGradient(
            0f, 0f, 0f, size.toFloat(),
            top, bottom, Shader.TileMode.CLAMP,
        )
        val bg = Paint().apply { shader = grad }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bg)

        val flagBlue = Color.parseColor(ArgPalette.CelesteHex)
        val flagYellow = Color.parseColor(ArgPalette.SunHex)
        val barH = 18f
        canvas.drawRect(0f, 0f, size.toFloat(), barH, Paint().apply { color = flagBlue })
        canvas.drawRect(0f, size - barH, size.toFloat(), size.toFloat(), Paint().apply { color = flagYellow })

        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 280f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(emoji, size / 2f, 320f, emojiPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 64f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        wrapAndDrawText(canvas, message, textPaint, RectF(80f, 380f, size - 80f, 880f))

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Anti-gastos boludos · 🇦🇷", size / 2f, 970f, tagPaint)

        return bmp
    }

    private fun renderDailyStoryBitmap(subtitle: String, personaEmoji: String, quote: String, mood: CopyMood): Bitmap {
        val w = 1080
        val h = 1920
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val (top, bottom, accent, _) = palette(mood)
        val grad = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            top, bottom, Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = grad })

        val flagBlue = Color.parseColor(ArgPalette.CelesteHex)
        val flagYellow = Color.parseColor(ArgPalette.SunHex)
        val barH = 22f
        canvas.drawRect(0f, 0f, w.toFloat(), barH, Paint().apply { color = flagBlue })
        canvas.drawRect(0f, h - barH, w.toFloat(), h.toFloat(), Paint().apply { color = flagYellow })

        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 220f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(personaEmoji, w / 2f, 420f, emojiPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 52f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Mi día", w / 2f, 520f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }
        wrapAndDrawText(canvas, subtitle, subPaint, RectF(72f, 560f, w - 72f, 720f))

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 48f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        wrapAndDrawText(canvas, quote, bodyPaint, RectF(64f, 780f, w - 64f, 1500f))

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Anti-gastos boludos · 🇦🇷", w / 2f, h - 120f, tagPaint)
        return bmp
    }

    private fun renderMonthlyRecapStoryBitmap(
        monthLabel: String,
        totalPesos: Long,
        personaEmoji: String,
        personaName: String,
        highlights: List<String>,
        closingQuote: String,
        mood: CopyMood,
    ): Bitmap {
        val w = 1080
        val h = 1920
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val (top, bottom, accent, _) = palette(mood)
        val grad = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            top, bottom, Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = grad })

        val flagBlue = Color.parseColor(ArgPalette.CelesteHex)
        val flagYellow = Color.parseColor(ArgPalette.SunHex)
        val barH = 22f
        canvas.drawRect(0f, 0f, w.toFloat(), barH, Paint().apply { color = flagBlue })
        canvas.drawRect(0f, h - barH, w.toFloat(), h.toFloat(), Paint().apply { color = flagYellow })

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 56f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("📊 Mi recap", w / 2f, 200f, titlePaint)
        val monthPaint = Paint(titlePaint).apply { textSize = 80f }
        canvas.drawText(monthLabel, w / 2f, 300f, monthPaint)

        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 200f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(personaEmoji, w / 2f, 530f, emojiPaint)

        val personaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 38f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(personaName, w / 2f, 600f, personaPaint)

        val totalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 38f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Total del mes", w / 2f, 720f, totalLabelPaint)

        val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 96f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(MoneyFormat.formatPesos(totalPesos), w / 2f, 820f, totalPaint)

        val bulletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 42f
            textAlign = Paint.Align.LEFT
        }
        var y = 940f
        highlights.take(4).forEach { line ->
            wrapAndDrawText(
                canvas = canvas,
                text = "• $line",
                paint = bulletPaint,
                rect = RectF(80f, y, w - 80f, y + 130f),
                centerVertically = false,
            )
            y += 130f
        }

        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 46f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
        }
        wrapAndDrawText(canvas, "“$closingQuote”", quotePaint, RectF(72f, 1500f, w - 72f, 1760f))

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Anti-gastos boludos · 🇦🇷", w / 2f, h - 80f, tagPaint)
        return bmp
    }

    private fun wrapAndDrawText(canvas: Canvas, text: String, paint: Paint, rect: RectF) {
        wrapAndDrawText(canvas, text, paint, rect, centerVertically = true)
    }

    private fun wrapAndDrawText(canvas: Canvas, text: String, paint: Paint, rect: RectF, centerVertically: Boolean) {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (w in words) {
            val candidate = if (current.isEmpty()) w else "$current $w"
            if (paint.measureText(candidate) <= rect.width()) {
                current = StringBuilder(candidate)
            } else {
                lines += current.toString()
                current = StringBuilder(w)
            }
        }
        if (current.isNotEmpty()) lines += current.toString()

        val lineH = paint.textSize * 1.2f
        val totalH = lines.size * lineH
        var y = if (centerVertically) {
            rect.top + (rect.height() - totalH) / 2 + paint.textSize
        } else {
            rect.top + paint.textSize
        }
        val cx = (rect.left + rect.right) / 2
        for (line in lines) {
            val x = if (paint.textAlign == Paint.Align.CENTER) cx else rect.left
            canvas.drawText(line, x, y, paint)
            y += lineH
        }
    }

    private data class Palette(val top: Int, val bottom: Int, val accent: Int, val emoji: String)

    private fun palette(mood: CopyMood): Palette = when (mood) {
        CopyMood.CHEER -> Palette(
            Color.parseColor("#E6F4EA"),
            Color.parseColor("#B7E1C0"),
            Color.parseColor("#1B5E20"),
            "🥳",
        )
        CopyMood.NEUTRAL -> Palette(
            Color.parseColor("#E7F1FB"),
            Color.parseColor("#B6D4F1"),
            Color.parseColor("#0D47A1"),
            "🧉",
        )
        CopyMood.BURN -> Palette(
            Color.parseColor("#FFF4E0"),
            Color.parseColor("#FFD68A"),
            Color.parseColor("#8B5A00"),
            "🔥",
        )
        CopyMood.ALARM -> Palette(
            Color.parseColor("#FDE7EA"),
            Color.parseColor("#F4A0AB"),
            Color.parseColor("#8C0E1E"),
            "🚨",
        )
    }
}
