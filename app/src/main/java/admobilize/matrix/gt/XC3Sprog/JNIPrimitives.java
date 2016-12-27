package admobilize.matrix.gt.XC3Sprog;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import admobilize.matrix.gt.Config;
import admobilize.matrix.gt.R;

/**
 * Created by Antonio Vanegas @hpsaturn on 12/23/16.
 */

public class JNIPrimitives {

    private static final String TAG = JNIPrimitives.class.getSimpleName();
    private static final boolean DEBUG = Config.DEBUG;

    private final Context ctx;
    private String sytemPath;

    public JNIPrimitives(Context ctx) {
        this.ctx = ctx;
    }

    public interface OnSystemLoadListener {
        void onSuccess(int msg);

        void onError(String err);
    }

    public void init(OnSystemLoadListener onSystemLoadListener) {
        loadBinaryFile();
        loadFirmware(onSystemLoadListener, sytemPath);
    }

    public void loadBinaryFile() {
        try {
            InputStream fis = ctx.getResources().openRawResource(R.raw.system);
            File cascadeDir = ctx.getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "faces.bin");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            fis.close();
            os.close();

            if (DEBUG) Log.i(TAG, "loadBinary, path: " + mCascadeFile.getAbsolutePath());
            if (DEBUG) Log.i(TAG, "loadBinary, space: " + mCascadeFile.getTotalSpace() / 8);

            this.sytemPath = mCascadeFile.getAbsolutePath();

        } catch (IOException e) {
            if (DEBUG) Log.e(TAG, "loadBinaryFile IOException:");
            e.printStackTrace();
        }
    }

    //
    // Native JNI
    //

    static {
        System.loadLibrary("xc3loader");
    }

    private static native int loadFirmware(OnSystemLoadListener onSystemLoadListener, String path);

    public static native int burnFirmware();

    public static native void stopLoader();

}
