package com.arvind.diaryai.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arvind.diaryai.data.Schema
import com.arvind.diaryai.data.StorageManager
import com.arvind.diaryai.databinding.ActivityDiaryMetricsBinding

class DiaryMetricsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiaryMetricsBinding
    private lateinit var storage: StorageManager
    private val moodOptions = (Schema.MOOD_MIN..Schema.MOOD_MAX).map { it.toString() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiaryMetricsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        storage = StorageManager(this)

        binding.tvDate.text = StorageManager.today()
        binding.spMood.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, moodOptions)
        binding.spMood.setSelection(moodOptions.indexOf("0"))

        Schema.ROUTE_OPTIONS.forEach { option ->
            val cb = CheckBox(this).apply { text = option; tag = option }
            binding.routeGroup.addView(cb)
        }

        loadExistingIfAny()
        binding.btnSaveMetrics.setOnClickListener { save() }
    }

    /** If today's row already exists, load it so the user is editing, not duplicating. */
    private fun loadExistingIfAny() {
        val rows = storage.readRows(Schema.FILE_DIARY_METRICS, Schema.DIARY_METRICS_HEADER)
        val today = rows.lastOrNull { it["diary_date"] == StorageManager.today() } ?: return
        binding.etFamily.setText(today["family_social"])
        binding.etExam.setText(today["exam_prep_study"])
        binding.etBook.setText(today["book_writing_business_prep"])
        binding.etWorkEarn.setText(today["work_to_earn_today"])
        binding.etDailyTarget.setText(today["daily_target"])
        binding.rbTarget.rating = (today["target_achieved"]?.toFloatOrNull() ?: 0f)
        binding.sbWorkIntense.progress = ((today["work_intense"]?.toIntOrNull() ?: 1) - 1).coerceIn(0, 4)
        val mood = today["mood"] ?: "0"
        binding.spMood.setSelection(moodOptions.indexOf(mood).coerceAtLeast(0))
        val routeSelected = today["route_location"]?.split(",")?.map { it.trim() } ?: emptyList()
        for (i in 0 until binding.routeGroup.childCount) {
            val cb = binding.routeGroup.getChildAt(i) as CheckBox
            cb.isChecked = routeSelected.contains(cb.tag)
        }
    }

    private fun save() {
        val selectedRoutes = (0 until binding.routeGroup.childCount)
            .map { binding.routeGroup.getChildAt(it) as CheckBox }
            .filter { it.isChecked }
            .joinToString(", ") { it.text.toString() }

        val newRow = listOf(
            StorageManager.today(),
            binding.etFamily.text.toString(),
            binding.etExam.text.toString(),
            binding.etBook.text.toString(),
            binding.etWorkEarn.text.toString(),
            binding.etDailyTarget.text.toString(),
            binding.rbTarget.rating.toInt().toString(),
            selectedRoutes,
            (binding.sbWorkIntense.progress + 1).toString(),
            binding.spMood.selectedItem.toString()
        )

        // Upsert: replace today's row if present, else append.
        val existing = storage.readRows(Schema.FILE_DIARY_METRICS, Schema.DIARY_METRICS_HEADER)
        val updated = existing.filter { it["diary_date"] != StorageManager.today() }
            .map { row -> Schema.DIARY_METRICS_HEADER.map { row[it] ?: "" } } + listOf(newRow)
        storage.writeAllRows(Schema.FILE_DIARY_METRICS, Schema.DIARY_METRICS_HEADER, updated)

        Toast.makeText(this, "Today's metrics saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
