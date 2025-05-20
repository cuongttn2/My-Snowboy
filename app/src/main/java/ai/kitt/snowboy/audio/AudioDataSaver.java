package ai.kitt.snowboy.audio;

import android.content.Context;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import ai.kitt.snowboy.Constants;

public class AudioDataSaver implements AudioDataReceivedListener {
    private static final String TAG = AudioDataSaver.class.getSimpleName();

    // file size limit (in bytes) after subtracting initial header
    private static final float MAX_MB = 50f;
    private static final float INIT_MB = 1.3f;
    private static final int MAX_BYTES =
            (int)((MAX_MB - INIT_MB) * 1024 * 1024);

    private int sizeCounter = 0;
    private File saveFile;
    private DataOutputStream dos;

    /**
     * @param context ?? tìm filesDir và t?o th? m?c "snowboy"
     */
    public AudioDataSaver(Context context) {
        File workDir = new File(context.getFilesDir(), Constants.ASSETS_RES_DIR);
        if (!workDir.exists()) {
            if (!workDir.mkdirs()) {
                Log.e(TAG, "Cannot create workDir: " + workDir);
            }
        }
        saveFile = new File(workDir, "recording.pcm");
        Log.i(TAG, "Audio will be saved to " + saveFile.getAbsolutePath());
    }

    @Override
    public void start() {
        // ??m b?o th? m?c ?ã có
        File parent = saveFile.getParentFile();
        if (!parent.exists()) parent.mkdirs();

        // xóa file c?
        if (saveFile.exists()) saveFile.delete();
        try {
            saveFile.createNewFile();
            BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(saveFile)
            );
            dos = new DataOutputStream(bos);
            sizeCounter = 0;
        } catch (IOException e) {
            Log.e(TAG, "IOException creating audio file", e);
            throw new IllegalStateException("Cannot open file for writing", e);
        }
    }

    @Override
    public void onAudioDataReceived(byte[] data, int length) {
        if (dos == null) return;
        try {
            // quay file m?i n?u quá l?n
            if (sizeCounter + length > MAX_BYTES) {
                stop();
                start();
            }
            dos.write(data, 0, length);
            sizeCounter += length;
        } catch (IOException e) {
            Log.e(TAG, "IOException writing audio data", e);
        }
    }

    @Override
    public void stop() {
        if (dos != null) {
            try {
                dos.close();
                Log.i(TAG, "Recording saved to " + saveFile);
            } catch (IOException e) {
                Log.e(TAG, "IOException closing audio file", e);
            }
            dos = null;
        }
    }
}
