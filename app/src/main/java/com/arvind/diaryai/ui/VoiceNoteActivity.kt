package com.arvind.diaryai.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.arvind.diaryai.data.Schema
import com.arvind.diaryai.data.StorageManager
import com.arvind.diaryai.databinding.ActivityVoiceNoteBinding
import com.arvind.diaryai.util.TranscriptProcessor

/**
 * Uses Android's built-in Google voice input (SpeechRecognizer) -- the same engine behind
 * the mic button on your keyboard. No models to bundle, no setup on your end.
 *
 * Fully offline recognition: on the phone, open the Google app -> Settings -> Voice ->
 * Offline speech recognition -> download the language(s) you want (English / Hindi). Once
 * downloaded, EXTRA_PREFER_OFFLINE below makes it recognize speech without a data connection.
 * Without that download, it silently falls back to using a network connection when available.
 *
 * Flow: pick language -> tap record -> speak (start with "Diary"/"Idea"/"Thoughts") ->
 * transcript appears EDITABLE -> review/edit -> Submit (nothing is saved before that).
 */
class VoiceNoteActivity : AppCompatActivity(), RecognitionListener {

    private lateinit var binding: ActivityVoiceNoteBinding
    private lateinit var storage: StorageManager
    private var recognizer: SpeechRecognizer? = null
    private var recording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        storage = StorageManager(this)

        binding.spLanguage.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, Schema.NOTE_LANGUAGES.map { it.first }
        )

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        binding.btnRecord.setOnClickListener { if (recording) stopRecording() else startRecording() }
        binding.btnSubmit.setOnClickListener { submit() }
    }

    private fun startRecording() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "No speech recognizer available on this device", Toast.LENGTH_LONG).show()
            return
        }
        val localeTag = Schema.NOTE_LANGUAGES[binding.spLanguage.selectedItemPosition].second
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { it.setRecognitionListener(this) }
        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) // uses on-device model if downloaded
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer?.startListening(intent)
        recording = true
        binding.btnRecord.text = "\u25a0 Stop"
        binding.tvStatus.text = "Listening..."
    }

    private fun stopRecording() {
        recognizer?.stopListening()
        recording = false
        binding.btnRecord.text = "\ud83c\udfa4 Start Recording"
    }

    override fun onResults(results: Bundle?) {
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        if (text.isNotBlank()) applyTranscript(text)
        recording = false
        binding.btnRecord.text = "\ud83c\udfa4 Start Recording"
    }

    override fun onError(error: Int) {
        recording = false
        binding.btnRecord.text = "\ud83c\udfa4 Start Recording"
        binding.tvStatus.text = "Didn't catch that -- tap to try again."
    }

    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onReadyForSpeech(params: Bundle?) { binding.tvStatus.text = "Listening..." }
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() { binding.tvStatus.text = "Processing..." }
    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun applyTranscript(rawText: String) {
        val (type, cleaned) = TranscriptProcessor.detectTypeAndClean(rawText)
        binding.tvType.text = type
        val prior = binding.etTranscript.text?.toString().orEmpty()
        binding.etTranscript.setText(if (prior.isBlank()) cleaned else "$prior $cleaned")
        binding.tvStatus.text = "Review below, edit if needed, then Submit."
        binding.btnSubmit.isEnabled = true
    }

    private fun submit() {
        val finalText = binding.etTranscript.text.toString().trim()
        if (finalText.isBlank()) return
        val type = binding.tvType.text?.toString()?.ifBlank { "Diary" } ?: "Diary"
        val language = Schema.NOTE_LANGUAGES[binding.spLanguage.selectedItemPosition].first

        storage.appendRow(
            Schema.FILE_NOTES, Schema.NOTES_HEADER,
            listOf(StorageManager.today(), StorageManager.nowTime(), type, finalText, language)
        )
        Toast.makeText(this, "Saved as $type", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        recognizer?.destroy()
        super.onDestroy()
    }
}
