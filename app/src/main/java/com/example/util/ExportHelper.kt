package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ExportHelper {
    private fun ensureExportDir(context: Context): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun shareTextFile(
        context: Context,
        filename: String,
        mimeType: String,
        content: String,
        chooserTitle: String = "Export from ImmunoVerse"
    ): Result<String> = runCatching {
        val dir = ensureExportDir(context)
        val outFile = File(dir, filename.replace(Regex("[^A-Za-z0-9._-]"), "_"))
        outFile.writeText(content, Charsets.UTF_8)
        shareFile(context, outFile, mimeType, chooserTitle)
        outFile.name
    }

    fun exportChartPng(
        context: Context,
        filename: String,
        title: String,
        series: List<Pair<String, List<Pair<Double, Double>>>>
    ): Result<String> = runCatching {
        val dir = ensureExportDir(context)
        val safeName = filename.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val outFile = File(dir, if (safeName.endsWith(".png")) safeName else "$safeName.png")

        val w = 1200
        val h = 720
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(5, 9, 20))

        val palette = listOf(
            Color.rgb(34, 229, 255),
            Color.rgb(139, 107, 255),
            Color.rgb(61, 220, 151),
            Color.rgb(255, 200, 87),
            Color.rgb(224, 79, 176)
        )
        val gridPaint = Paint().apply {
            color = Color.argb(45, 120, 200, 255)
            strokeWidth = 2f
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(230, 241, 255)
            textSize = 28f
            isFakeBoldText = true
        }
        val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 22f
        }
        canvas.drawText(title, 56f, 54f, textPaint)

        val padL = 80f
        val padR = w - 48f
        val padT = 92f
        val padB = h - 96f
        for (g in 0..4) {
            val gy = padT + (padB - padT) * g / 4f
            canvas.drawLine(padL, gy, padR, gy, gridPaint)
        }

        val allPts = series.flatMap { it.second }
        val minX = allPts.minOfOrNull { it.first } ?: 0.0
        val maxX = (allPts.maxOfOrNull { it.first } ?: 1.0).coerceAtLeast(minX + 1e-6)
        val minY = 0.0
        val maxY = (allPts.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(0.1)

        series.forEachIndexed { idx, (label, pts) ->
            if (pts.isEmpty()) return@forEachIndexed
            val col = palette[idx % palette.size]
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = col
                strokeWidth = 4.5f
                style = Paint.Style.STROKE
            }
            val path = Path()
            pts.forEachIndexed { pIdx, (xv, yv) ->
                val sx = padL + ((xv - minX) / (maxX - minX)).toFloat() * (padR - padL)
                val sy = padB - ((yv - minY) / (maxY - minY)).toFloat() * (padB - padT)
                if (pIdx == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
            }
            canvas.drawPath(path, linePaint)
            legendPaint.color = col
            canvas.drawText("● $label", padL + idx * 210f, h - 34f, legendPaint)
        }

        FileOutputStream(outFile).use { fos -> bmp.compress(Bitmap.CompressFormat.PNG, 95, fos) }
        bmp.recycle()
        shareFile(context, outFile, "image/png", "Share Chart PNG")
        outFile.name
    }

    fun exportAndSharePdf(
        context: Context,
        filename: String,
        title: String,
        sections: List<Pair<String, String>>,
        chooserTitle: String = "Share PDF Report"
    ): Result<String> = runCatching {
        val dir = ensureExportDir(context)
        val safeName = filename.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val outFile = File(dir, if (safeName.endsWith(".pdf")) safeName else "$safeName.pdf")

        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 42f
        val maxWidth = pageWidth - margin * 2

        val bgPaint = Paint().apply { color = Color.WHITE }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(10, 32, 72)
            textSize = 18f
            isFakeBoldText = true
        }
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(14, 98, 128)
            textSize = 13f
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(32, 38, 48)
            textSize = 10.5f
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            textSize = 8.5f
        }

        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas: Canvas = page.canvas
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)
        var y = margin + 12f
        canvas.drawText(title.take(70), margin, y, titlePaint)
        y += 22f
        canvas.drawText("ImmunoVerse 2.0 Scientific Report • Educational & Research Output", margin, y, footerPaint)
        y += 22f

        fun newPage() {
            canvas.drawText("Page $pageNum", pageWidth - margin - 35f, pageHeight - 24f, footerPaint)
            doc.finishPage(page)
            pageNum++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
            canvas = page.canvas
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)
            y = margin + 10f
        }

        for ((heading, body) in sections) {
            if (y > pageHeight - 90f) newPage()
            canvas.drawText(heading, margin, y, headingPaint)
            y += 16f
            for (paragraph in body.lines()) {
                val words = paragraph.split(" ")
                var line = StringBuilder()
                for (w in words) {
                    val candidate = if (line.isEmpty()) w else "$line $w"
                    if (bodyPaint.measureText(candidate) > maxWidth) {
                        if (y > pageHeight - 50f) newPage()
                        canvas.drawText(line.toString(), margin, y, bodyPaint)
                        y += 14f
                        line = StringBuilder(w)
                    } else {
                        line = StringBuilder(candidate)
                    }
                }
                if (line.isNotEmpty()) {
                    if (y > pageHeight - 50f) newPage()
                    canvas.drawText(line.toString(), margin, y, bodyPaint)
                    y += 14f
                }
            }
            y += 10f
        }
        canvas.drawText("Page $pageNum", pageWidth - margin - 35f, pageHeight - 24f, footerPaint)
        doc.finishPage(page)

        FileOutputStream(outFile).use { fos -> doc.writeTo(fos) }
        doc.close()
        shareFile(context, outFile, "application/pdf", chooserTitle)
        outFile.name
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
