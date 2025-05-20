package com.example.mysnowboy.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Handler
import android.os.Process
import android.util.Log
import com.example.mysnowboy.Constants
import com.example.mysnowboy.MsgEnum
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RecordingThread(handler: Handler?, listener: AudioDataReceivedListener?) {
    private var shouldContinue = false
    private var listener: AudioDataReceivedListener? = null
    private var handler: Handler? = null
    private var thread: Thread? = null

    private val activeModel: String = strEnvWorkSpace + ACTIVE_UMDL
    private val commonRes: String = strEnvWorkSpace + ACTIVE_RES

    private val detector: SnowboyDetect = SnowboyDetect(commonRes, activeModel)
    private val player = MediaPlayer()

    init {
        this.handler = handler
        this.listener = listener

        detector.SetSensitivity("0.6")
        detector.SetAudioGain(1)
        detector.ApplyFrontend(true)
        try {
            player.setDataSource(strEnvWorkSpace + "ding.wav")
            player.prepare()
        } catch (e: IOException) {
            Log.e(TAG, "Playing ding sound error", e)
        }
    }

    private fun sendMessage(what: MsgEnum, obj: Any?) {
        if (null != handler) {
            val msg = handler!!.obtainMessage(what.ordinal(), obj)
            handler!!.sendMessage(msg)
        }
    }

    fun startRecording() {
        if (thread != null) return

        shouldContinue = true
        thread = Thread(object : Runnable {
            override fun run() {
                record()
            }
        })
        thread!!.start()
    }

    fun stopRecording() {
        if (thread == null) return

        shouldContinue = false
        thread = null
    }

    private fun record() {
        Log.v(TAG, "Start")
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

        // Buffer size in bytes: for 0.1 second of audio
        var bufferSize = (Constants.SAMPLE_RATE * 0.1 * 2) as Int
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = Constants.SAMPLE_RATE * 2
        }

        val audioBuffer = ByteArray(bufferSize)
        val record = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT,
            Constants.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Audio Record can't initialize!")
            return
        }
        record.startRecording()
        if (null != listener) {
            listener!!.start()
        }
        Log.v(TAG, "Start recording")

        var shortsRead: Long = 0
        detector.Reset()
        while (shouldContinue) {
            record.read(audioBuffer, 0, audioBuffer.size)

            if (null != listener) {
                listener!!.onAudioDataReceived(audioBuffer, audioBuffer.size)
            }


            // Converts to short array.
            val audioData = ShortArray(audioBuffer.size / 2)
            ByteBuffer.wrap(audioBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                .get(audioData)

            shortsRead += audioData.size.toLong()

            // Snowboy hotword detection.
            val result: Int = detector.RunDetection(audioData, audioData.size)

            if (result == -2) {
                // post a higher CPU usage:
                // sendMessage(MsgEnum.MSG_VAD_NOSPEECH, null);
            } else if (result == -1) {
                sendMessage(MsgEnum.MSG_ERROR, "Unknown Detection Error")
            } else if (result == 0) {
                // post a higher CPU usage:
                // sendMessage(MsgEnum.MSG_VAD_SPEECH, null);
            } else if (result > 0) {
                sendMessage(MsgEnum.MSG_ACTIVE, null)
                Log.i("Snowboy: ", "Hotword " + result.toString() + " detected!")
                player.start()
            }
        }

        record.stop()
        record.release()

        if (null != listener) {
            listener!!.stop()
        }
        Log.v(TAG, String.format("Recording stopped. Samples read: %d", shortsRead))
    }

    companion object {
        init {
            System.loadLibrary("snowboy-detect-android")
        }

        private val TAG: String = RecordingThread::class.java.getSimpleName()

        private val ACTIVE_RES: String = Constants.ACTIVE_RES
        private val ACTIVE_UMDL: String = Constants.ACTIVE_UMDL

        private val strEnvWorkSpace: String? = Constants.DEFAULT_WORK_SPACE
    }
}