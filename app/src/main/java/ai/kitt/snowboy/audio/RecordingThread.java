package ai.kitt.snowboy.audio;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import ai.kitt.snowboy.AppResCopy;
import ai.kitt.snowboy.Constants;
import ai.kitt.snowboy.MsgEnum;
import ai.kitt.snowboy.SnowboyDetect;

public class RecordingThread {
    static {
        System.loadLibrary("snowboy-detect-android");
    }

    private static final String TAG = RecordingThread.class.getSimpleName();

    private final SnowboyDetect detector;
    private final MediaPlayer player;
    private final Handler handler;
    private final AudioDataReceivedListener listener;

    private Thread thread;
    private volatile boolean running;

    public RecordingThread(Context context,
                           Handler handler,
                           AudioDataReceivedListener listener) {
        this.handler = handler;
        this.listener = listener;

        // 1) Copy common.res + model vào internal storage
        File workDir = AppResCopy.copyResToInternal(context);
        String resPath = new File(workDir, Constants.ACTIVE_RES).getAbsolutePath();
        String modelPath = new File(workDir, Constants.ACTIVE_UMDL).getAbsolutePath();

        // 2) Kh?i t?o SnowboyDetect
        detector = new SnowboyDetect(resPath, modelPath);
        detector.SetSensitivity("0.6");
        detector.SetAudioGain(1.0f);
        detector.ApplyFrontend(true);

        // 3) Chu?n b? ti?ng ding.wav
        player = new MediaPlayer();
        try {
            File dingFile = new File(workDir, "ding.wav");
            player.setDataSource(dingFile.getAbsolutePath());
            player.prepare();
        } catch (IOException e) {
            Log.e(TAG, "Error preparing ding.wav", e);
        }
    }

    private void sendMessage(MsgEnum what) {
        if (handler != null) {
            Message msg = handler.obtainMessage(what.ordinal());
            handler.sendMessage(msg);
        }
    }

    public void startRecording() {
        if (thread != null && thread.isAlive()) return;
        running = true;
        thread = new Thread(this::recordLoop, "Snowboy-Record");
        thread.start();
    }

    public void stopRecording() {
        running = false;
        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
            thread = null;
        }
    }

    @SuppressLint("MissingPermission")
    private void recordLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

        // 1. Tính buffer size
        int sampleRate = Constants.SAMPLE_RATE;
        int minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        int bufferSize = Math.max(minBuf, sampleRate * 2);   // ít nh?t 2*sampleRate byte

        // 2. Kh?i t?o AudioRecord v?i mic phù h?p
        AudioRecord recorder = new AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,  // ho?c MIC
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );
        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Cannot initialize AudioRecord");
            return;
        }

        // 3. B?t ??u ghi
        recorder.startRecording();
        if (listener != null) listener.start();
        Log.v(TAG, "Recording started");

        byte[] audioBuffer = new byte[bufferSize];

        // 4. Reset detector tr??c khi vòng loop
        detector.Reset();

        while (running) {
            int readBytes = recorder.read(audioBuffer, 0, audioBuffer.length);
            if (readBytes < 0) break;

            // 5. G?i audio thô cho listener (n?u b?n mu?n l?u file PCM)
            if (listener != null) {
                listener.onAudioDataReceived(audioBuffer, readBytes);
            }

            // 6. Chuy?n sang short[] ?? feed Snowboy
            short[] samples = new short[readBytes / 2];
            ByteBuffer.wrap(audioBuffer, 0, readBytes)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()
                    .get(samples);

            // 7. Ch?y detection
            int result = detector.RunDetection(samples, samples.length);
            if (result > 0) {
                Log.i(TAG, "Hotword detected: index=" + result);
                sendMessage(MsgEnum.MSG_ACTIVE);
                player.start();
            } else if (result == -1) {
                sendMessage(MsgEnum.MSG_ERROR);
            }
            // (b? qua result==0 và result==-2)
        }

        // 8. D?ng
        recorder.stop();
        recorder.release();
        if (listener != null) listener.stop();
        Log.v(TAG, "Recording stopped");
    }
}
