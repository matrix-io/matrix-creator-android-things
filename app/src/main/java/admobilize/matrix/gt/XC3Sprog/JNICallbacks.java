package admobilize.matrix.gt.XC3Sprog;

import android.content.Context;
import android.os.Build;

import admobilize.matrix.gt.Config;

/**
 * Created by Antonio Vanegas @hpsaturn on 12/23/16.
 */

public class JNICallbacks {

    private static final String TAG = JNICallbacks.class.getSimpleName();
    private static final boolean DEBUG = Config.DEBUG;

    private final JNIPrimitives.OnSystemLoadListener onSystemLoadListener;


    public JNICallbacks(Context ctx) {
        this.onSystemLoadListener = (JNIPrimitives.OnSystemLoadListener) ctx;
    }

    private void onFirmwareLoad(int success){
        onSystemLoadListener.onSuccess(success);
    }


    static public String getBuildVersion() {
        return Build.VERSION.RELEASE;
    }

    public long getRuntimeMemorySize() {
        return Runtime.getRuntime().freeMemory();
    }


}
