package com.example.mysnowboy

import android.os.Environment
import java.io.File

object Constants {
    const val ASSETS_RES_DIR: String = "snowboy"
    val DEFAULT_WORK_SPACE: String =
        Environment.getExternalStorageDirectory().getAbsolutePath() + "/snowboy/"
    const val ACTIVE_UMDL: String = "alexa.umdl"
    const val ACTIVE_RES: String = "common.res"
    val SAVE_AUDIO: String = DEFAULT_WORK_SPACE + File.separatorChar + "recording.pcm"
    const val SAMPLE_RATE: Int = 16000
}