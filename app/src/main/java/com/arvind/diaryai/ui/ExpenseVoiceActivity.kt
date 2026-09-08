package com.arvind.diaryai.ui

import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arvind.diaryai.data.Schema
import com.arvind.diaryai.data.StorageManager
import com.arvind.diaryai.databinding.ActivityExpenseVoiceBinding
import com.arvind.diaryai.util.ExpenseVoiceParser

/** Same built-in Google voice input as VoiceNoteActivity -- no extra setup needed. */
class ExpenseVoiceActivity : AppCompatActivity(), RecognitionListener {

    private lateinit var binding: ActivityExpenseVoiceBinding
    private lateinit var storage: StorageManager
    private var recognizer: SpeechRecognizer? = null
    private var recording = false

    private val batch = mutableListOf<List<String>>()
    private lateinit var batchAdapter: ArrayAdapter<String>
    private val batchLabels = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseVoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        storage = StorageManager(this)

        binding.spCashOnline.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Schema.EXPENSE_CASH_ONLINE)
        binding.spWhose.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Schema.EXPENSE_WHOSE)
        binding.spCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Schema.EXPENSE_CATEGORIES)

        batchAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, batchLabels)
        binding.lvBatch.adapter = batchAdapter

        binding.btnRecord.setOnClickListener { if (recording) stopRecording() else startRecording() }
        binding.btnAddToBatch.setOnClickListener { addCurrentToBatch() }
        binding.btnSaveAll.setOnClickListener { saveAll() }
    }

    private fun startRecording() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "No speech recognizer available on this device", Toast.LENGTH_LONG).show()
            return
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { it.setRecognitionListener(this) }
        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer?.startListening(intent)
        recording = true
        binding.btnRecord.text = "\u25a0 Stop"
    }

    private fun stopRecording() {
        recognizer?.stopListening()
        recording = false
        binding.btnRecord.text = "\ud83c\udfa4 Speak Entry"
    }

    override fun onResults(results: Bundle?) {
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        recording = false
        binding.btnRecord.text = "\ud83c\udfa4 Speak Entry"
        if (text.isBlank()) return
        val parsed = ExpenseVoiceParser.parse(text)
        binding.etAmount.setText(parsed.amount)
        setSpinner(binding.spCashOnline, Schema.EXPENSE_CASH_ONLINE, parsed.cashOnline)
        setSpinner(binding.spWhose, Schema.EXPENSE_WHOSE, parsed.whose)
        setSpinner(binding.spCategory, Schema.EXPENSE_CATEGORIES, parsed.category)
        binding.etComments.setText(parsed.comments)
        Toast.makeText(this, "Parsed -- review fields, then tap Add entry", Toast.LENGTH_SHORT).show()
    }

    override fun onError(error: Int) {
        recording = false
        binding.btnRecord.text = "\ud83c\udfa4 Speak Entry"
    }
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun setSpinner(spinner: android.widget.Spinner, options: List<String>, value: String) {
        val idx = options.indexOf(value)
        if (idx >= 0) spinner.setSelection(idx)
    }

    private fun addCurrentToBatch() {
        val amount = binding.etAmount.text.toString().ifBlank { "0" }
        val row = listOf(
            StorageManager.today(), StorageManager.nowTime(),
            amount, binding.spCashOnline.selectedItem.toString(),
            binding.spWhose.selectedItem.toString(), binding.spCategory.selectedItem.toString(),
            binding.etComments.text.toString()
        )
        batch.add(row)
        batchLabels.add("$amount  ${row[5]}  ${row[6]}")
        batchAdapter.notifyDataSetChanged()

        binding.etAmount.text?.clear()
        binding.etComments.text?.clear()
    }

    private fun saveAll() {
        if (binding.etAmount.text?.isNotBlank() == true) addCurrentToBatch()
        batch.forEach { storage.appendRow(Schema.FILE_EXPENSES, Schema.EXPENSE_HEADER, it) }
        Toast.makeText(this, "${batch.size} entries saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        recognizer?.destroy()
        super.onDestroy()
    }
}
