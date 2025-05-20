package ai.kitt.snowboy.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import ai.kitt.snowboy.Constants;

public class PlaybackThread {
    private static final String TAG = PlaybackThread.class.getSimpleName();

    private final File recordFile;
    private Thread thread;
    private volatile boolean shouldContinue;
    private AudioTrack audioTrack;

    /**
     * @param context ?? xác ??nh filesDir
     */
    public PlaybackThread(Context context) {
        // Th? m?c workDir là filesDir/snowboy
        File workDir = new File(context.getFilesDir(), Constants.ASSETS_RES_DIR);
        recordFile = new File(workDir, "recording.pcm");
        Log.i(TAG, "Playback will read from " + recordFile.getAbsolutePath());
    }

    public boolean playing() {
        return thread != null && thread.isAlive();
    }

    public void startPlayback() {
        if (playing()) return;
        shouldContinue = true;
        thread = new Thread(this::playLoop, "Snowboy-Play");
        thread.start();
    }

    public void stopPlayback() {
        shouldContinue = false;
        if (thread != null) {
            try { thread.join(); } catch (InterruptedException ignored) {}
            thread = null;
        }
        releaseAudioTrack();
    }

    private void releaseAudioTrack() {
        if (audioTrack != null) {
            try { audioTrack.release(); }
            catch (Exception ignored) {}
            audioTrack = null;
        }
    }

    private void playLoop() {
        short[] samples = readPCM();
        if (samples == null || samples.length == 0) {
            Log.e(TAG, "No PCM data to play");
            return;
        }

        int bufferSizeInBytes = samples.length * (Short.SIZE/Byte.SIZE);
        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                Constants.SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSizeInBytes,
                AudioTrack.MODE_STREAM
        );

        if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            Log.e(TAG, "AudioTrack init failed");
            return;
        }

        audioTrack.play();
        int offset = 0;
        while (shouldContinue && offset < samples.length) {
            int toWrite = Math.min(1024, samples.length - offset);
            audioTrack.write(samples, offset, toWrite);
            offset += toWrite;
        }
        releaseAudioTrack();
    }

    private short[] readPCM() {
        if (!recordFile.exists()) {
            Log.e(TAG, "Cannot find saved audio file at " + recordFile);
            return null;
        }
        try (
                DataInputStream dis = new DataInputStream(
                        new BufferedInputStream(new FileInputStream(recordFile))
                )
        ) {
            byte[] audioData = new byte[(int) recordFile.length()];
            dis.readFully(audioData);

            ShortBuffer sb = ByteBuffer
                    .wrap(audioData)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer();

            short[] samples = new short[sb.remaining()];
            sb.get(samples);
            return samples;

        } catch (IOException e) {
            Log.e(TAG, "IO error reading PCM", e);
            return null;
        }
    }
}
