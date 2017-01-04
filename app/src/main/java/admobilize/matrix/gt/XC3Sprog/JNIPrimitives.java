package admobilize.matrix.gt.XC3Sprog;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import admobilize.matrix.gt.BoardDefaults;
import admobilize.matrix.gt.Config;
import admobilize.matrix.gt.R;


/**
 * Created by Antonio Vanegas @hpsaturn on 12/23/16.
 */

public class JNIPrimitives {

    private static final String TAG = JNIPrimitives.class.getSimpleName();
    private static final boolean DEBUG = Config.DEBUG;

    private final Context ctx;
    private PeripheralManagerService service;
    private SpiDevice spiDevice;
    private String sytemPath;

    private Gpio mXCProgTDI;
    private Gpio mXCProgTMS;
    private Gpio mXCProgTCK;
    private Gpio mXCProgTDO;
    private Gpio mXCProgSAM;
    private Gpio mLedGpio;
    private ByteBuffer buf;


    public JNIPrimitives(Context ctx, PeripheralManagerService service, SpiDevice spiDevice) {
        this.ctx = ctx;
        this.service = service;
        this.spiDevice = spiDevice;
    }

    public interface OnSystemLoadListener {
        void onSuccess(int msg);
        void onError(String err);
    }

    public void txrx_block(byte[]tdo, byte[]tdi, int length, boolean finalBlock, boolean last){
        int i = 0;
        int j = 0;
        byte tdo_byte = 0;
        byte tdi_byte =0;

        if(length>10000){
           buf.get(tdi,0,length);
        }

        int tdilenght = tdi.length;
        int tdolenght = tdo.length;


//        if(DEBUG&&tdilenght>0)Log.d(TAG,"TXRX_BLOCK IN ==>TDI="+byteArrayToHex(tdi));
//        if(DEBUG&&tdolenght>0)Log.d(TAG,"TXRX_BLOCK IN ==>TDO="+byteArrayToHex(tdo));

        if(DEBUG)Log.d(TAG,"==> tdi_lenght: "+tdilenght);
        if (tdilenght>0) tdi_byte = tdi[j];

//        LOGD ("lenght %d",length);
//        LOGD ("last %d",last);
        while (i < length - 1) {
            tdo_byte = (byte) (tdo_byte + (txrx(false, (tdi_byte & 1) == 1) << (i % 8)));
            if (tdilenght>0) tdi_byte = (byte) (tdi_byte >> 1);
            i++;
            if ((i % 8) == 0) {            // Next byte
                if(tdolenght>0)tdo[j] = tdo_byte;  // Save the TDO byte
                tdo_byte = 0;
                j++;
                if (tdilenght>0) tdi_byte = tdi[j];  // Get the next TDI byte
            }
        }
        if(!finalBlock)return;
        tdo_byte = (byte) (tdo_byte + (txrx(last, (tdi_byte & 1) == 1) << (i % 8)));
        if(tdolenght>0)tdo[j] = tdo_byte;
//        if(DEBUG)Log.d(TAG,"TXRX_BLOCK OUT ==> TDO="+byteArrayToHex(tdo));
//        if(DEBUG)Log.d(TAG,"TXRX_BLOCK OUT ==> TDO Length: "+tdo.length);
//        if(DEBUG)Log.d(TAG,"TXRX_BLOCK OUT ==> writeTCK(false)");
        writeTCK(false);
    }

    public byte txrx(boolean tms, boolean tdi){
//        if(DEBUG)Log.d(TAG,"TXRX:");
        tx(tms, tdi);
        if(readTDO())return 1;
        else return 0;
    }

    public void tx(boolean tms, boolean tdi) {
//        if(DEBUG)Log.d(TAG,"TX:");
        writeTCK(false);
        writeTDI(tdi);
        writeTMS(tms);
        writeTCK(true);
    }

    public void tx_tms(byte[] pat, int length, int force) {
//        if(DEBUG)Log.d(TAG,"TX_TMS:");
        int i;
        byte tms = 0;
        for (i = 0; i < length; i++) {
            if ((i & 0x7) == 0) tms = pat[i >> 3];
            if((tms & 0x01) == 0) tx(false, true);
            else tx(true,true);
            tms = (byte) (tms >> 1);
        }
        writeTCK(false);
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /*
    public int onFirmwareLoad(){
        return 0;
    }
    */

    public void writeTDI(boolean state) {
        try {
            mXCProgTDI.setValue(state);
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
            e.printStackTrace();
        }
    }

    public void writeTMS(boolean state) {
        try {
            mXCProgTMS.setValue(state);
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
            e.printStackTrace();
        }
    }

    public void writeTCK(boolean state) {
        try {
            mXCProgTCK.setValue(state);
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
            e.printStackTrace();
        }
    }

    public boolean readTDO() {
        try {
            return mXCProgTDO.getValue();
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
            e.printStackTrace();
        }
        return false;
    }

    public void writeLED(boolean state) {
        try {
            mLedGpio.setValue(state);
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
            e.printStackTrace();
        }
    }

    public boolean readLED() {
        try {
            return mLedGpio.getValue();
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
            e.printStackTrace();
        }
        return false;
    }

    public void resetSAM(){
        try {
            mXCProgSAM.setValue(true);
            mXCProgSAM.setValue(false);
            mXCProgSAM.setValue(true);

        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
            e.printStackTrace();
        }
    }

    public void init() {
        try {
            Log.i(TAG, "Available GPIO: " + service.getGpioList());
            mLedGpio = service.openGpio(BoardDefaults.getGPIOForLED());
            mXCProgTDI = service.openGpio(BoardDefaults.getGPIO_TDI());
            mXCProgTMS = service.openGpio(BoardDefaults.getGPIO_TMS());
            mXCProgTCK = service.openGpio(BoardDefaults.getGPIO_TCK());
            mXCProgTDO = service.openGpio(BoardDefaults.getGPIO_TDO());
            mXCProgSAM = service.openGpio(BoardDefaults.getGPIO_SAM());
            mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mXCProgTDI.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mXCProgTMS.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mXCProgTCK.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mXCProgSAM.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mXCProgTDO.setDirection(Gpio.DIRECTION_IN);
            resetSAM();
            loadBinaryFile();
            buf = ByteBuffer.allocateDirect(1000*3000).order(ByteOrder.nativeOrder());
            loadFirmware(this,sytemPath,buf);
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
            e.printStackTrace();
        }
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

    private void onFirmwareLoad(int success){
        if(DEBUG)Log.i(TAG,"onFirmwareLoad: "+success);
    }

    public static String getBuildVersion() {
        return Build.VERSION.RELEASE;
    }

    public static long getRuntimeMemorySize() {
        return Runtime.getRuntime().freeMemory();
    }

    //
    // Native JNI
    //

    static {
        System.loadLibrary("xc3loader");
    }

    private native static int loadFirmware(JNIPrimitives object, String path, ByteBuffer buf);

    public native static int burnFirmware();

    public native static void stopLoader();

}
