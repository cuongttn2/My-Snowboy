package com.example.mysnowboy

import ai.kitt.snowboy.MsgEnum.MSG_ACTIVE
import android.annotation.SuppressLint
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.Html
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ai.kitt.snowboy.AppResCopy
import ai.kitt.snowboy.MsgEnum
import ai.kitt.snowboy.MsgEnum.MSG_ACTIVE
import ai.kitt.snowboy.MsgEnum.MSG_ERROR
import ai.kitt.snowboy.MsgEnum.MSG_INFO
import ai.kitt.snowboy.MsgEnum.MSG_VAD_NOSPEECH
import ai.kitt.snowboy.MsgEnum.MSG_VAD_SPEECH
import ai.kitt.snowboy.audio.AudioDataSaver
import ai.kitt.snowboy.audio.PlaybackThread
import ai.kitt.snowboy.audio.RecordingThread

class MainActivity : AppCompatActivity() {
    private var record_button: Button? = null
    private var play_button: Button? = null
    private var log: TextView? = null
    private var logView: ScrollView? = null

    var strLog: String? = null

    private var preVolume = -1

    private
    var activeTimes: Long = 0

    private var recordingThread: RecordingThread? = null
    private var playbackThread: PlaybackThread? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
//        setUI()
//
//        setProperVolume()
//
//        AppResCopy.copyResFromAssetsToSD(this)
//
//        activeTimes = 0
//        recordingThread = RecordingThread(handle, AudioDataSaver())
//        playbackThread = PlaybackThread()
    }

    fun showToast(msg: CharSequence?) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun setUI() {
        record_button = findViewById<View?>(R.id.btn_test1) as Button
        record_button!!.setOnClickListener(record_button_handle)
        record_button!!.setEnabled(true)

        play_button = findViewById<View?>(R.id.btn_test2) as Button
        play_button!!.setOnClickListener(play_button_handle)
        play_button!!.setEnabled(true)

        log = findViewById<View?>(R.id.log) as TextView
        logView = findViewById<View?>(R.id.logView) as ScrollView
    }

    private fun setMaxVolume() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        preVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        updateLog(" ----> preVolume = " + preVolume, "green")
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        updateLog(" ----> maxVolume = " + maxVolume, "green")
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        updateLog(" ----> currentVolume = " + currentVolume, "green")
    }

    private fun setProperVolume() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        preVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        updateLog(" ----> preVolume = " + preVolume, "green")
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        updateLog(" ----> maxVolume = " + maxVolume, "green")
        val properVolume = (maxVolume.toFloat() * 0.2).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, properVolume, 0)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        updateLog(" ----> currentVolume = " + currentVolume, "green")
    }

    private fun restoreVolume() {
        if (preVolume >= 0) {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, preVolume, 0)
            updateLog(" ----> set preVolume = " + preVolume, "green")
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            updateLog(" ----> currentVolume = " + currentVolume, "green")
        }
    }

    private fun startRecording() {
        recordingThread?.startRecording()
        updateLog(" ----> recording started ...", "green")
        record_button?.setText(R.string.btn1_stop)
    }

    private fun stopRecording() {
        recordingThread?.stopRecording()
        updateLog(" ----> recording stopped ", "green")
        record_button!!.setText(R.string.btn1_start)
    }

    private fun startPlayback() {
        updateLog(" ----> playback started ...", "green")
        play_button?.setText(R.string.btn2_stop)
        // (new PcmPlayer()).playPCM();
        playbackThread?.startPlayback()
    }

    private fun stopPlayback() {
        updateLog(" ----> playback stopped ", "green")
        play_button!!.setText(R.string.btn2_start)
        playbackThread?.stopPlayback()
    }

    private fun sleep() {
        try {
            Thread.sleep(500)
        } catch (e: Exception) {
        }
    }

    private val record_button_handle: View.OnClickListener = object : View.OnClickListener {
        // @Override
        override fun onClick(arg0: View?) {
            if (record_button!!.getText() == getResources().getString(R.string.btn1_start)) {
                stopPlayback()
                sleep()
                startRecording()
            } else {
                stopRecording()
                sleep()
            }
        }
    }

    private val play_button_handle: View.OnClickListener = object : View.OnClickListener {
        // @Override
        override fun onClick(arg0: View?) {
            if (play_button!!.getText() == getResources().getString(R.string.btn2_start)) {
                stopRecording()
                sleep()
                startPlayback()
            } else {
                stopPlayback()
            }
        }
    }

    var handle: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            val message = MsgEnum.getMsgEnum(msg.what)
            when (message) {
                MSG_ACTIVE -> {
                    activeTimes++
                    updateLog(" ----> Detected " + activeTimes + " times", "green")
                    // Toast.makeText(Demo.this, "Active "+activeTimes, Toast.LENGTH_SHORT).show();
                    showToast("Active " + activeTimes)
                }

                MSG_INFO -> updateLog(" ----> " + message)
                MSG_VAD_SPEECH -> updateLog(" ----> normal voice", "blue")
                MSG_VAD_NOSPEECH -> updateLog(" ----> no speech", "blue")
                MSG_ERROR -> updateLog(" ----> " + msg.toString(), "red")
                else -> super.handleMessage(msg)
            }
        }
    }

    fun updateLog(text: String) {
        log!!.post(object : Runnable {
            override fun run() {
                if (currLogLineNum >= MAX_LOG_LINE_NUM) {
                    val st = strLog!!.indexOf("<br>")
                    strLog = strLog!!.substring(st + 4)
                } else {
                    currLogLineNum++
                }
                val str = "<font color='white'>" + text + "</font>" + "<br>"
                strLog = if (strLog == null || strLog!!.length == 0) str else strLog + str
                log!!.setText(Html.fromHtml(strLog))
            }
        })
        logView!!.post(object : Runnable {
            override fun run() {
                logView!!.fullScroll(ScrollView.FOCUS_DOWN)
            }
        })
    }


    var MAX_LOG_LINE_NUM: Int = 200

    var currLogLineNum: Int = 0

    fun updateLog(text: String?, color: String) {
        log!!.post(object : Runnable {
            override fun run() {
                if (currLogLineNum >= MAX_LOG_LINE_NUM) {
                    val st = strLog!!.indexOf("<br>")
                    strLog = strLog!!.substring(st + 4)
                } else {
                    currLogLineNum++
                }
                val str = "<font color='" + color + "'>" + text + "</font>" + "<br>"
                strLog = if (strLog == null || strLog!!.length == 0) str else strLog + str
                log!!.setText(Html.fromHtml(strLog))
            }
        })
        logView!!.post(object : Runnable {
            override fun run() {
                logView!!.fullScroll(ScrollView.FOCUS_DOWN)
            }
        })
    }

    private fun emptyLog() {
        strLog = null
        log!!.setText("")
    }

    public override fun onDestroy() {
        restoreVolume()
        recordingThread?.stopRecording()
        super.onDestroy()
    }
}