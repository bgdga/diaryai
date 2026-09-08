package com.arvind.diaryai.util

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.arvind.diaryai.data.Schema
import com.arvind.diaryai.data.StorageManager
import java.io.File
import java.io.FileOutputStream

/**
 * Builds the monthly PDF exactly as specified:
 *   Page 1: month dashboard summary
 *   Page 2: category-vs-date expense breakdown
 *   Page 3+: daily diary panels, then ideas, then thoughts (blank days skipped for idea/thought)
 * A4 at 72dpi = 595 x 842 pt.
 */
class PdfExportUtil(private val context: Context) {

    private val storage = StorageManager(context)
    private val pageW = 595
    private val pageH = 842
    private val margin = 40
    private val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
    private val headerPaint = Paint().apply { textSize = 14f; isFakeBoldText = true }
    private val bodyPaint = Paint().apply { textSize = 11f }

    fun exportMonth(yearMonth: String): File {
        val doc = PdfDocument()
        val range = DateRange(yearMonth + "-01", yearMonth + "-31")

        val expenses = storage.readRows(Schema.FILE_EXPENSES, Schema.EXPENSE_HEADER).filter { range.contains(it["date"] ?: "") }
        val metrics = storage.readRows(Schema.FILE_DIARY_METRICS, Schema.DIARY_METRICS_HEADER).filter { range.contains(it["diary_date"] ?: "") }
        val notes = storage.readRows(Schema.FILE_NOTES, Schema.NOTES_HEADER).filter { range.contains(it["date"] ?: "") }

        addDashboardPage(doc, yearMonth, expenses, metrics)
        addExpensePage(doc, expenses)
        addDiarySections(doc, metrics, notes)

        val outFile = File(context.getExternalFilesDir(null), "DiaryAI_${yearMonth}.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    private fun newPage(doc: PdfDocument): Pair<PdfDocument.Page, Int> {
        val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, doc.pages.size + 1).create())
        return page to margin
    }

    private fun addDashboardPage(doc: PdfDocument, month: String, expenses: List<Map<String, String>>, metrics: List<Map<String, String>>) {
        val (page, _) = newPage(doc)
        val c = page.canvas
        var y = margin + 20
        c.drawText("Monthly Dashboard — $month", margin.toFloat(), y.toFloat(), titlePaint); y += 30

        val spent = -expenses.filter { (it["amount"]?.toDoubleOrNull() ?: 0.0) < 0 }.sumOf { it["amount"]?.toDoubleOrNull() ?: 0.0 }
        val earned = expenses.filter { (it["amount"]?.toDoubleOrNull() ?: 0.0) > 0 }.sumOf { it["amount"]?.toDoubleOrNull() ?: 0.0 }
        val avgMood = metrics.mapNotNull { it["mood"]?.toDoubleOrNull() }.average().takeIf { !it.isNaN() } ?: 0.0
        val avgIntensity = metrics.mapNotNull { it["work_intense"]?.toDoubleOrNull() }.average().takeIf { !it.isNaN() } ?: 0.0
        val avgTarget = metrics.mapNotNull { it["target_achieved"]?.toDoubleOrNull() }.average().takeIf { !it.isNaN() } ?: 0.0

        listOf(
            "Total spent: ${"%.0f".format(spent)}",
            "Total earned: ${"%.0f".format(earned)}",
            "Net: ${"%.0f".format(earned - spent)}",
            "Diary days logged: ${metrics.size}",
            "Average mood: ${"%.2f".format(avgMood)}",
            "Average work intensity: ${"%.2f".format(avgIntensity)}",
            "Average target achieved: ${"%.2f".format(avgTarget)} / 5"
        ).forEach { line -> c.drawText(line, margin.toFloat(), y.toFloat(), bodyPaint); y += 20 }

        y += 10
        c.drawText("Spend by category:", margin.toFloat(), y.toFloat(), headerPaint); y += 20
        expenses.filter { (it["amount"]?.toDoubleOrNull() ?: 0.0) < 0 }
            .groupBy { it["category"] ?: "Other" }
            .mapValues { (_, rows) -> -rows.sumOf { it["amount"]?.toDoubleOrNull() ?: 0.0 } }
            .toList().sortedByDescending { it.second }
            .forEach { (cat, amt) ->
                c.drawText("  $cat: ${"%.0f".format(amt)}", margin.toFloat(), y.toFloat(), bodyPaint); y += 16
            }
        doc.finishPage(page)
    }

    private fun addExpensePage(doc: PdfDocument, expenses: List<Map<String, String>>) {
        val (page, _) = newPage(doc)
        val c = page.canvas
        var y = margin + 20
        c.drawText("Expenses — Category vs Date", margin.toFloat(), y.toFloat(), titlePaint); y += 30
        expenses.sortedBy { it["date"] }.forEach { row ->
            if (y > pageH - margin) return@forEach // simple v1: truncates if month has a very high entry count
            c.drawText("${row["date"]}  ${row["category"]}  ${row["amount"]}  (${row["cash_online"]}, ${row["whose_exp"]})",
                margin.toFloat(), y.toFloat(), bodyPaint)
            y += 16
        }
        doc.finishPage(page)
    }

    private fun addDiarySections(doc: PdfDocument, metrics: List<Map<String, String>>, notes: List<Map<String, String>>) {
        // Daily diary metrics, one block per day
        var page = newPage(doc)
        var (pdfPage, _) = page
        var c = pdfPage.canvas
        var y = margin + 20
        c.drawText("Daily Diary", margin.toFloat(), y.toFloat(), titlePaint); y += 30

        fun ensureRoom(lines: Int) {
            if (y + lines * 16 > pageH - margin) {
                doc.finishPage(pdfPage)
                page = newPage(doc); pdfPage = page.first; c = pdfPage.canvas; y = margin + 20
            }
        }

        metrics.sortedBy { it["diary_date"] }.forEach { row ->
            ensureRoom(8)
            c.drawText(row["diary_date"] ?: "", margin.toFloat(), y.toFloat(), headerPaint); y += 18
            listOf(
                "Family+social: ${row["family_social"]}",
                "Exam/Study: ${row["exam_prep_study"]}",
                "Book/Business: ${row["book_writing_business_prep"]}",
                "Work to earn: ${row["work_to_earn_today"]}",
                "Target: ${row["daily_target"]} (achieved ${row["target_achieved"]}/5)",
                "Mood: ${row["mood"]}   Work intensity: ${row["work_intense"]}"
            ).forEach { c.drawText(it, (margin + 10).toFloat(), y.toFloat(), bodyPaint); y += 14 }
            y += 10
        }

        // Ideas, then Thoughts (skip days with no entry, per spec)
        listOf("Idea", "Thoughts").forEach { type ->
            ensureRoom(4)
            y += 10
            c.drawText(type, margin.toFloat(), y.toFloat(), titlePaint); y += 24
            notes.filter { it["type"] == type }.sortedBy { it["date"] }.forEach { row ->
                ensureRoom(3)
                c.drawText("${row["date"]} ${row["time"]}", (margin).toFloat(), y.toFloat(), headerPaint); y += 16
                c.drawText(row["transcript"] ?: "", (margin + 10).toFloat(), y.toFloat(), bodyPaint); y += 18
            }
        }
        doc.finishPage(pdfPage)
    }
}
