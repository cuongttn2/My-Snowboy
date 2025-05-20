package com.example.mysnowboy.audio

import android.util.Log
import com.example.mysnowboy.Constants
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class AudioDataSaver : AudioDataReceivedListener {
    // file size of when to delete and create a new recording file
    private val MAX_RECORDING_FILE_SIZE_IN_MB = 50f

    // initial file size of recording file
    private val INITIAL_FILE_SIZE_IN_MB = 1.3f

    // converted max file size
    private val MAX_RECORDING_FILE_SIZE_IN_BYTES =
        (MAX_RECORDING_FILE_SIZE_IN_MB - INITIAL_FILE_SIZE_IN_MB) * 1024 * 1024

    // keeps track of recording file size
    private var recordingFileSizeCounterInBytes = 0

    private var saveFile: File? = null
    private var dataOutputStreamInstance: DataOutputStream? = null

    init {
        saveFile = File(Constants.SAVE_AUDIO)
        Log.e(TAG, Constants.SAVE_AUDIO)
    }

    override fun start() {
        if (null != saveFile) {
            if (saveFile!!.exists()) {
                saveFile!!.delete()
            }
            try {
                saveFile!!.createNewFile()
            } catch (e: IOException) {
                Log.e(TAG, "IO Exception on creating audio file " + saveFile.toString(), e)
            }

            try {
                val bufferedStreamInstance = BufferedOutputStream(
                    FileOutputStream(this.saveFile)
                )
                dataOutputStreamInstance = DataOutputStream(bufferedStreamInstance)
            } catch (e: FileNotFoundException) {
                throw IllegalStateException("Cannot Open File", e)
            }
        }
    }

    override fun onAudioDataReceived(data: ByteArray?, length: Int) {
        try {
            if (null != dataOutputStreamInstance) {
                if (recordingFileSizeCounterInBytes >= MAX_RECORDING_FILE_SIZE_IN_BYTES) {
                    stop()
                    start()
                    recordingFileSizeCounterInBytes = 0
                }
                dataOutputStreamInstance!!.write(data, 0, length)
                recordingFileSizeCounterInBytes += length
            }
        } catch (e: IOException) {
            Log.e(TAG, "IO Exception on saving audio file " + saveFile.toString(), e)
        }
    }

    override fun stop() {
        if (null != dataOutputStreamInstance) {
            try {
                dataOutputStreamInstance!!.close()
            } catch (e: IOException) {
                Log.e(TAG, "IO Exception on finishing saving audio file " + saveFile.toString(), e)
            }
            Log.e(TAG, "Recording saved to " + saveFile.toString())
        }
    }

    companion object {
        private val TAG: String = AudioDataSaver::class.java.getSimpleName()
    }
}