package com.example.mysnowboy

import ai.kitt.snowboy.AppResCopy
import ai.kitt.snowboy.MsgEnum
import ai.kitt.snowboy.audio.AudioDataSaver
import ai.kitt.snowboy.audio.PlaybackThread
import ai.kitt.snowboy.audio.RecordingThread
import android.Manifest
import android.content.pm.PackageManager
import android.os.*
import android.text.Html
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var recordButton: Button
    private lateinit var playButton: Button
    private lateinit var logView: TextView
    private lateinit var logScroll: ScrollView

    private lateinit var audioSaver: AudioDataSaver
    private lateinit var recordingThread: RecordingThread
    private lateinit var playbackThread: PlaybackThread

    // t? qu?n lý tr?ng thái
    private var isRecording = false
    private var isPlaying   = false

    // Permission launcher
    private val micPermissionLaunch =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startRecording()
            else Toast.makeText(this, "C?n quy?n mic ?? ho?t ??ng", Toast.LENGTH_SHORT).show()
        }

    // Handler nh?n MsgEnum t? RecordingThread
    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (MsgEnum.values()[msg.what]) {
                MsgEnum.MSG_ACTIVE -> {
                    appendLog("Hotword detected!", "green")
                    Toast.makeText(this@MainActivity, "Hotword!", Toast.LENGTH_SHORT).show()
                }
                MsgEnum.MSG_ERROR -> appendLog("Detection error", "red")
                else             -> { /* b? qua */ }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // bind UI
        recordButton = findViewById(R.id.btn_test1)
        playButton   = findViewById(R.id.btn_test2)
        logView      = findViewById(R.id.log)
        logScroll    = findViewById(R.id.logView)

        recordButton.setOnClickListener { onRecordClicked() }
        playButton  .setOnClickListener { onPlayClicked() }
        recordButton.isEnabled = true
        playButton  .isEnabled = true

        // copy snowboy assets ? internal
        AppResCopy.copyResToInternal(this)

        // kh?i t?o AudioDataSaver KHÔNG truy?n tham s?
        audioSaver = AudioDataSaver(this)

        // kh?i t?o threads
        recordingThread = RecordingThread(this, handler, audioSaver)
        playbackThread  = PlaybackThread(this)
    }

    private fun onRecordClicked() {
        if (!isRecording) {
            // n?u ch?a có quy?n thì request
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                micPermissionLaunch.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                startRecording()
            }
        } else {
            stopRecording()
        }
    }

    private fun startRecording() {
        // d?ng playback n?u ?ang ch?y
        if (isPlaying) stopPlayback()

        recordingThread.startRecording()
        isRecording = true
        recordButton.text = getString(R.string.btn1_stop)
        appendLog("Recording started", "white")
    }

    private fun stopRecording() {
        recordingThread.stopRecording()
        isRecording = false
        recordButton.text = getString(R.string.btn1_start)
        appendLog("Recording stopped", "white")
    }

    private fun onPlayClicked() {
        if (!isPlaying) {
            // d?ng ghi n?u ?ang ch?y
            if (isRecording) stopRecording()
            startPlayback()
        } else {
            stopPlayback()
        }
    }

    private fun startPlayback() {
        playbackThread.startPlayback()
        isPlaying = true
        playButton.text = getString(R.string.btn2_stop)
        appendLog("Playback started", "white")
    }

    private fun stopPlayback() {
        playbackThread.stopPlayback()
        isPlaying = false
        playButton.text = getString(R.string.btn2_start)
        appendLog("Playback stopped", "white")
    }

    private fun appendLog(text: String, color: String) {
        // Gi?i h?n 200 dòng
        val sb = StringBuilder(strLog ?: "")
        if (sb.lineSequence().count() >= 200) {
            // c?t dòng ??u
            val idx = sb.indexOf("<br>")
            if (idx >= 0) sb.delete(0, idx + 4)
        }
        sb.append("<font color='$color'>$text</font><br>")
        strLog = sb.toString()
        logView.text = Html.fromHtml(strLog, Html.FROM_HTML_MODE_LEGACY)
        logScroll.post { logScroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private var strLog: String? = null

    override fun onDestroy() {
        recordingThread.stopRecording()
        playbackThread.stopPlayback()
        super.onDestroy()
    }
}
