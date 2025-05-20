package com.example.mysnowboy.audio

interface AudioDataReceivedListener {
    fun start()
    fun onAudioDataReceived(data: ByteArray?, length: Int)
    fun stop()
}