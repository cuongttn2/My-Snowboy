package ai.kitt.snowboy;

import android.os.Environment;

import java.io.File;

public class Constants {
    public static final String ASSETS_RES_DIR = "appu";
    public static final String DEFAULT_WORK_SPACE = Environment.getExternalStorageDirectory().getAbsolutePath() + "/appu/";
    public static final String APPU_UMDL = "appu.pmdl";
    public static final String JARVIS_UMDL = "JARVIS.pmdl";
    public static final String ALEXA_UMDL = "alexa.umdl";
    public static final String ACTIVE_RES = "common.res";
    public static final String SAVE_AUDIO = Constants.DEFAULT_WORK_SPACE + File.separatorChar + "recording.pcm";
    public static final int SAMPLE_RATE = 16000;
}
