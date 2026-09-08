package com.arvind.diaryai.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Local-only TSV storage. No network calls anywhere in this class.
 * Files live under: <app external files>/DiaryAI/*.tsv
 * Opening any file in this folder with Excel/Sheets works directly since values are tab-separated.
 */
class StorageManager(private val context: Context) {

    private val rootDir: File by lazy {
        File(context.getExternalFilesDir(null), Schema.DIR_NAME).apply { mkdirs() }
    }
    val audioDir: File by lazy {
        File(rootDir, Schema.DIR_AUDIO).apply { mkdirs() }
    }

    private fun fileFor(name: String, header: List<String>): File {
        val f = File(rootDir, name)
        if (!f.exists()) {
            f.writeText(header.joinToString("\t") + "\n")
        }
        return f
    }

    private fun escapeTsv(value: String): String =
        value.replace("\t", " ").replace("\n", " / ").trim()

    fun appendRow(fileName: String, header: List<String>, values: List<String>) {
        require(values.size == header.size) { "Row has ${values.size} cols, expected ${header.size}" }
        val f = fileFor(fileName, header)
        f.appendText(values.joinToString("\t") { escapeTsv(it) } + "\n")
    }

    /** Returns all rows (excluding header) as list of column maps. */
    fun readRows(fileName: String, header: List<String>): List<Map<String, String>> {
        val f = fileFor(fileName, header)
        val lines = f.readLines()
        if (lines.size <= 1) return emptyList()
        return lines.drop(1).filter { it.isNotBlank() }.map { line ->
            val cols = line.split("\t")
            header.indices.associate { i -> header[i] to (cols.getOrNull(i) ?: "") }
        }
    }

    /** Overwrite an entire file (used e.g. to upsert today's single diary_metrics row). */
    fun writeAllRows(fileName: String, header: List<String>, rows: List<List<String>>) {
        val f = File(rootDir, fileName)
        val sb = StringBuilder()
        sb.append(header.joinToString("\t")).append("\n")
        rows.forEach { row -> sb.append(row.joinToString("\t") { escapeTsv(it) }).append("\n") }
        f.writeText(sb.toString())
    }

    fun newAudioFile(type: String): File {
        val ts = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return File(audioDir, "${ts}_${type}Diary.wav")
    }

    companion object {
        fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        fun nowTime(): String = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        fun yearMonth(dateStr: String): String = dateStr.take(7) // "yyyy-MM"
    }
}
