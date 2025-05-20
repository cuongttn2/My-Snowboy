package com.example.mysnowboy.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.example.mysnowboy.Constants
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.lang.Byte
import java.lang.Short
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.Boolean
import kotlin.ByteArray
import kotlin.Exception
import kotlin.ShortArray
import kotlin.String

class PlaybackThread {
    private var thread: Thread? = null
    private var shouldContinue = false
    protected var audioTrack: AudioTrack? = null

    fun playing(): Boolean {
        return thread != null
    }

    fun startPlayback() {
        if (thread != null) return

        // Start streaming in a thread
        shouldContinue = true
        thread = Thread(object : Runnable {
            override fun run() {
                play()
            }
        })
        thread!!.start()
    }

    fun stopPlayback() {
        if (thread == null) return

        shouldContinue = false
        relaseAudioTrack()
        thread = null
    }

    protected fun relaseAudioTrack() {
        if (audioTrack != null) {
            try {
                audioTrack!!.release()
            } catch (e: Exception) {
            }
        }
    }

    fun readPCM(): ShortArray {
        try {
            val recordFile: File = File(Constants.SAVE_AUDIO)
            val inputStream: InputStream = FileInputStream(recordFile)
            val bufferedInputStream = BufferedInputStream(inputStream)
            val dataInputStream = DataInputStream(bufferedInputStream)

            val audioData = ByteArray(recordFile.length().toInt())

            dataInputStream.read(audioData)
            dataInputStream.close()
            Log.v(TAG, "audioData size: " + audioData.size)

            val sb = ByteBuffer.wrap(audioData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            val samples = ShortArray(sb.limit() - sb.position())
            sb.get(samples)
            return samples
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Cannot find saved audio file", e)
        } catch (e: IOException) {
            Log.e(TAG, "IO Exception on saved audio file", e)
        }
        return null
    }

    private fun play() {
        val samples = this.readPCM()
        val shortSizeInBytes = Short.SIZE / Byte.SIZE
        val bufferSizeInBytes = samples.size * shortSizeInBytes
        Log.v(
            TAG,
            "shortSizeInBytes: " + shortSizeInBytes + " bufferSizeInBytes: " + bufferSizeInBytes
        )

        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            Constants.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSizeInBytes,
            AudioTrack.MODE_STREAM
        )

        if (audioTrack!!.getState() == AudioTrack.STATE_INITIALIZED) {
            audioTrack!!.play()
            audioTrack!!.write(samples, 0, samples.size)
            Log.v(TAG, "Audio playback started")
        }

        if (!shouldContinue) {
            relaseAudioTrack()
        }
    }

    companion object {
        private val TAG: String = PlaybackThread::class.java.getSimpleName()
    }
}