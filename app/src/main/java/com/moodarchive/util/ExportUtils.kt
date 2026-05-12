package com.moodarchive.util

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.moodarchive.domain.model.DiaryEntry
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Утилита для экспорта записей дневника в форматы PDF и Markdown.
 */
object ExportUtils {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru"))

    /**
     * Экспортирует записи в Markdown-файл и открывает диалог "Поделиться".
     */
    fun exportToMarkdown(context: Context, entries: List<DiaryEntry>) {
        val sb = StringBuilder()
        sb.appendLine("# MoodArchive — Дневник эмоций")
        sb.appendLine("_Экспортировано: ${dateFormat.format(Date())}_")
        sb.appendLine()

        entries.sortedByDescending { it.createdAt }.forEach { entry ->
            sb.appendLine("---")
            sb.appendLine("## ${entry.emotion.emoji} ${entry.emotion.displayName} — ${dateFormat.format(Date(entry.createdAt))}")
            sb.appendLine()
            sb.appendLine(entry.text)
            if (entry.attachments.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("**Вложения:** ${entry.attachments.joinToString(", ") { it.fileName.ifBlank { it.type.name } }}")
            }
            sb.appendLine()
        }

        val file = File(context.cacheDir, "moodarchive_export.md")
        FileWriter(file).use { it.write(sb.toString()) }
        shareFile(context, file, "text/markdown")
    }

    /**
     * Экспортирует записи в PDF-файл через Canvas и открывает диалог "Поделиться".
     */
    fun exportToPdf(context: Context, entries: List<DiaryEntry>) {
        val pdf = PdfDocument()
        val pageWidth = 595  // A4 ширина в points
        val pageHeight = 842 // A4 высота в points
        val margin = 40f
        val lineHeight = 20f
        val titlePaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply { textSize = 13f }
        val metaPaint = Paint().apply {
            textSize = 11f
            alpha = 150
        }

        var pageNumber = 1
        var yPos = margin + 30f
        var currentPage = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = currentPage.canvas

        // Заголовок
        canvas.drawText("MoodArchive — Дневник эмоций", margin, yPos, titlePaint)
        yPos += lineHeight * 2

        fun newPageIfNeeded() {
            if (yPos > pageHeight - margin * 2) {
                pdf.finishPage(currentPage)
                pageNumber++
                yPos = margin + 20f
                currentPage = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = currentPage.canvas
            }
        }

        if (entries.isEmpty()) {
            canvas.drawText("Нет записей для экспорта", margin, yPos, bodyPaint)
            yPos += lineHeight
        }

        entries.sortedByDescending { it.createdAt }.forEach { entry ->
            newPageIfNeeded()
            val header = "${entry.emotion.displayName} · ${dateFormat.format(Date(entry.createdAt))}"
            canvas.drawText(header, margin, yPos, titlePaint)
            yPos += lineHeight

            // Текст записи — разбиваем по строкам
            val words = entry.text.split(" ")
            var line = ""
            words.forEach { word ->
                val testLine = if (line.isEmpty()) word else "$line $word"
                if (bodyPaint.measureText(testLine) < pageWidth - margin * 2) {
                    line = testLine
                } else {
                    newPageIfNeeded()
                    canvas.drawText(line, margin, yPos, bodyPaint)
                    yPos += lineHeight
                    line = word
                }
            }
            if (line.isNotEmpty()) {
                newPageIfNeeded()
                canvas.drawText(line, margin, yPos, bodyPaint)
                yPos += lineHeight
            }

            if (entry.attachments.isNotEmpty()) {
                newPageIfNeeded()
                val attachInfo = "Вложения: ${entry.attachments.joinToString(", ") { it.fileName.ifBlank { it.type.name } }}"
                canvas.drawText(attachInfo, margin, yPos, metaPaint)
                yPos += lineHeight
            }

            yPos += lineHeight * 0.5f
        }

        pdf.finishPage(currentPage)

        val file = File(context.cacheDir, "moodarchive_export.pdf")
        file.outputStream().use { pdf.writeTo(it) }
        pdf.close()

        shareFile(context, file, "application/pdf")
    }

    /**
     * Открывает системный диалог "Поделиться" с файлом через FileProvider.
     * FLAG_ACTIVITY_NEW_TASK обязателен при вызове startActivity вне Activity.
     */
    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться записями").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
