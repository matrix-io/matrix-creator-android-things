package admobilize.matrix.gt.matrix;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;

import admobilize.matrix.gt.Config;

/**
 * Created by Antonio Vanegas @hpsaturn on 12/20/16.
 */

public class MicArray extends SensorBase {

    private static final String TAG = MicArray.class.getSimpleName();
    private static final boolean DEBUG = Config.DEBUG;

    public static final int kMicarrayBufferSize = 1024;
    public static final int kMicrophoneArrayIRQ = 6;
    public static final int kMicrophoneChannels = 8;
    public static final int kSamplingRate = 16000;

    int max_sample = 100;
    int sample=0;

    short[]output = new short[0];

    short[]channel0 = new short[128];
    short[]channel1 = new short[128];
    short[]channel2 = new short[128];
    short[]channel3 = new short[128];
    short[]channel4 = new short[128];
    short[]channel5 = new short[128];
    short[]channel6 = new short[128];
    short[]channel7 = new short[128];


    public MicArray(Wishbone wb) {
        super(wb);
    }

    public void read (){
        byte[] data = new byte[128*8*2];
        wb.SpiReadBurst((short) kMicrophoneArrayBaseAddress,data,128*8*2);
        for (int i=0;i<128;i++){
            channel0[i]=ByteBuffer.wrap(data,(i*8+0)*2,2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            channel1[i]=ByteBuffer.wrap(data,(i*8+1)*2,2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            channel2[i]=ByteBuffer.wrap(data,(i*8+2)*2,2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            channel3[i]=ByteBuffer.wrap(data,(i*8+3)*2,2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            channel4[i]=ByteBuffer.wrap(data,(i*8+4)*2,2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            channel5[i]=ByteBuffer.wrap(data,(i*8+5)*2,2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            channel6[i]=ByteBuffer.wrap(data,(i*8+6)*2,2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            channel7[i]=ByteBuffer.wrap(data,(i*8+7)*2,2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        }
        if(sample<max_sample){
           output=concat(output,channel0) ;
        }
        if(sample==max_sample){
//            write();  // TODO: not work! maybe GT not support EXTERNALSTORAGE permission
        }
        sample++;
    }

    public short[] concat(short[] a, short[] b) {
        int aLen = a.length;
        int bLen = b.length;
        short[] c= new short[aLen+bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    public void write () {
        try {
            if(DEBUG)Log.d(TAG,"isExternalStorageWritable: "+isExternalStorageWritable());
            if(DEBUG)Log.d(TAG,"isExternalStorageReadable: "+isExternalStorageReadable());
            if(DEBUG)Log.d(TAG,"write file..");
            File myFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath()+"/sound.txt");
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.write(Arrays.toString(output));
            myOutWriter.close();
            fOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

}
