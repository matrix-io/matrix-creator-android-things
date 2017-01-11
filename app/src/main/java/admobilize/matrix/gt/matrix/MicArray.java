package admobilize.matrix.gt.matrix;

import android.os.AsyncTask;
import android.util.Log;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import admobilize.matrix.gt.Config;

/**
 * Created by Antonio Vanegas @hpsaturn on 12/20/16.
 */

public class MicArray extends SensorBase {

    private static final String TAG = MicArray.class.getSimpleName();
    private static final boolean DEBUG = Config.DEBUG;

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
            new sendData().execute(); // only for debugging, receive data with netcat
            // TODO: write() SD write not work! maybe GT not support EXTERNALSTORAGE permission
//          write();
        }
        sample++;
    }

    private class sendData extends AsyncTask<Void,Void,Void>{
        @Override
        protected Void doInBackground(Void... voids) {
            writeViaSocket();
            return null;
        }
    }

    private void writeViaSocket(){
        if(DEBUG)Log.d(TAG,"write via socket..");
        if(DEBUG)Log.d(TAG,Arrays.toString(output));
        Socket socket = null;
        DataOutputStream dataOutputStream = null;
        DataInputStream dataInputStream = null;

        try {
            socket = new Socket(Config.EXTERNAL_DEBUG_IP, Config.EXTERNAL_DEBUG_PORT);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
            int length = output.length;
            for(int i=0;i<length;i++){
                dataOutputStream.writeShort(output[i]);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally{
            if (socket != null){
                try { socket.close(); } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (dataOutputStream != null){
                try { dataOutputStream.close(); } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (dataInputStream != null){
                try { dataInputStream.close(); } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public short[] concat(short[] a, short[] b) {
        int aLen = a.length;
        int bLen = b.length;
        short[] c= new short[aLen+bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

}
