package ai.kitt.snowboy;

import static ai.kitt.snowboy.Constants.ASSETS_RES_DIR;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AppResCopy {
    private final static String TAG = AppResCopy.class.getSimpleName();
    private static String envWorkSpace = Constants.DEFAULT_WORK_SPACE;

    public static File copyResToInternal(Context ctx) {
        // s? tr? v? th? m?c /data/data/?/files/snowboy
        File outDir = new File(ctx.getFilesDir(), ASSETS_RES_DIR);
        copyRecursive(ctx, ASSETS_RES_DIR, outDir, true);
        return outDir;
    }

    private static void copyRecursive(Context ctx, String src, File dst, boolean override) {
        try {
            String[] assets = ctx.getAssets().list(src);
            if (assets.length > 0) {
                if (!dst.exists()) dst.mkdirs();
                for (String name : assets) {
                    copyRecursive(ctx, src + "/" + name, new File(dst, name), override);
                }
            } else {
                // file
                if (dst.exists() && !override) return;
                try (InputStream is = ctx.getAssets().open(src);
                     FileOutputStream os = new FileOutputStream(dst)) {
                    byte[] buf = new byte[4096];
                    int r;
                    while ((r = is.read(buf)) > 0) os.write(buf, 0, r);
                }
                Log.i(TAG, "Copied " + src + " ? " + dst);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error copying " + src, e);
        }
    }

    private static void copyFilesFromAssets(Context context, String assetsSrcDir, String sdcardDstDir, boolean override) {
        try {
            String fileNames[] = context.getAssets().list(assetsSrcDir);
            if (fileNames.length > 0) {
                Log.i(TAG, assetsSrcDir +" directory has "+fileNames.length+" files.\n");
                File dir = new File(sdcardDstDir);
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        Log.e(TAG, "mkdir failed: "+sdcardDstDir);
                        return;
                    } else {
                        Log.i(TAG, "mkdir ok: "+sdcardDstDir);
                    }
                } else {
                    Log.w(TAG, sdcardDstDir+" already exists! ");
                }
                for (String fileName : fileNames) {
                    copyFilesFromAssets(context,assetsSrcDir + "/" + fileName,sdcardDstDir+"/"+fileName, override);
                }
            } else {
                Log.i(TAG, assetsSrcDir +" is file\n");
                File outFile = new File(sdcardDstDir);
                if (outFile.exists()) {
                    if (override) {
                        outFile.delete();
                        Log.e(TAG, "overriding file "+ sdcardDstDir +"\n");
                    } else {
                        Log.e(TAG, "file "+ sdcardDstDir +" already exists. No override.\n");
                        return;
                    }
                }
                InputStream is = context.getAssets().open(assetsSrcDir);
                FileOutputStream fos = new FileOutputStream(outFile);
                byte[] buffer = new byte[1024];
                int byteCount=0;
                while ((byteCount=is.read(buffer)) != -1) {
                    fos.write(buffer, 0, byteCount);
                }
                fos.flush();
                is.close();
                fos.close();
                Log.i(TAG, "copy to "+sdcardDstDir+" ok!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyResFromAssetsToSD(Context context) {
        copyFilesFromAssets(context, ASSETS_RES_DIR, envWorkSpace+"/", true);
    }
}