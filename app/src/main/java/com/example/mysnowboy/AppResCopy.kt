package com.example.mysnowboy

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object AppResCopy {
    private val TAG: String = AppResCopy::class.java.getSimpleName()
    private val envWorkSpace = Constants.DEFAULT_WORK_SPACE

    private fun copyFilesFromAssets(
        context: Context,
        assetsSrcDir: String,
        sdcardDstDir: String,
        override: Boolean
    ) {
        try {
            val fileNames = context.getAssets().list(assetsSrcDir)
            if (fileNames!!.size > 0) {
                Log.i(TAG, assetsSrcDir + " directory has " + fileNames.size + " files.\n")
                val dir = File(sdcardDstDir)
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        Log.e(TAG, "mkdir failed: " + sdcardDstDir)
                        return
                    } else {
                        Log.i(TAG, "mkdir ok: " + sdcardDstDir)
                    }
                } else {
                    Log.w(TAG, sdcardDstDir + " already exists! ")
                }
                for (fileName in fileNames) {
                    copyFilesFromAssets(
                        context,
                        assetsSrcDir + "/" + fileName,
                        sdcardDstDir + "/" + fileName,
                        override
                    )
                }
            } else {
                Log.i(TAG, assetsSrcDir + " is file\n")
                val outFile = File(sdcardDstDir)
                if (outFile.exists()) {
                    if (override) {
                        outFile.delete()
                        Log.e(TAG, "overriding file " + sdcardDstDir + "\n")
                    } else {
                        Log.e(TAG, "file " + sdcardDstDir + " already exists. No override.\n")
                        return
                    }
                }
                val `is` = context.getAssets().open(assetsSrcDir)
                val fos = FileOutputStream(outFile)
                val buffer = ByteArray(1024)
                var byteCount = 0
                while ((`is`.read(buffer).also { byteCount = it }) != -1) {
                    fos.write(buffer, 0, byteCount)
                }
                fos.flush()
                `is`.close()
                fos.close()
                Log.i(TAG, "copy to " + sdcardDstDir + " ok!")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun copyResFromAssetsToSD(context: Context) {
        copyFilesFromAssets(context, Constants.ASSETS_RES_DIR, envWorkSpace + "/", true)
    }
}