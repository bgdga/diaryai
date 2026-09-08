package com.arvind.diaryai.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arvind.diaryai.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVoiceNote.setOnClickListener {
            startActivity(Intent(this, VoiceNoteActivity::class.java))
        }
        binding.btnExpense.setOnClickListener {
            startActivity(Intent(this, ExpenseVoiceActivity::class.java))
        }
        binding.btnDiaryMetrics.setOnClickListener {
            startActivity(Intent(this, DiaryMetricsActivity::class.java))
        }
        binding.btnDashboard.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }
    }
}
