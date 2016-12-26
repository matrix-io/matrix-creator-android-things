package admobilize.matrix.gt.XC3Sprog;

import android.content.Context;
import android.os.Build;

import admobilize.matrix.gt.Config;
import admobilize.matrix.gt.MainActivity;

/**
 * Created by Antonio Vanegas @hpsaturn on 12/23/16.
 */

public class JNICallbacks {

    private static final String TAG = JNICallbacks.class.getSimpleName();
    private static final boolean DEBUG = Config.DEBUG;

    private final JNIPrimitives.OnSystemLoadListener onSystemLoadListener;
    private Context ctx = null;


    public JNICallbacks(Context ctx) {
        this.ctx=ctx;
        this.onSystemLoadListener = (JNIPrimitives.OnSystemLoadListener) ctx;
    }

    private void onFirmwareLoad(int success){
        onSystemLoadListener.onSuccess(success);
    }

    public void writeTDI(boolean state) {
        ((MainActivity)ctx).writeTDI(state);
    }

    public void writeTMS(boolean state) {
        ((MainActivity)ctx).writeTMS(state);
    }

    public void writeTCK(boolean state) {
        ((MainActivity)ctx).writeTCK(state);
    }

    public boolean readTDO() {
        return ((MainActivity)ctx).readTDO();
    }

    public void writeLED(boolean state) {
        ((MainActivity)ctx).writeLED(state);
    }

    public boolean readLED() {
        return ((MainActivity)ctx).readLED();
    }

    public static String getBuildVersion() {
        return Build.VERSION.RELEASE;
    }

    public long getRuntimeMemorySize() {
        return Runtime.getRuntime().freeMemory();
    }


}
