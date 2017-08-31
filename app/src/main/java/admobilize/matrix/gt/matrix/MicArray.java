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
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Iterator;

import admobilize.matrix.gt.Config;

/**
 * Created by Antonio Vanegas @hpsaturn on 12/20/16.
 */

public class MicArray extends SensorBase {

    private static final String TAG = MicArray.class.getSimpleName();
    private static final boolean DEBUG = Config.DEBUG;

    byte[] data = new byte[128*8*2];
    ArrayDeque<Short> mic0 = new ArrayDeque<>();
    ArrayDeque<Short> mic1 = new ArrayDeque<>();
    ArrayDeque<Short> mic2 = new ArrayDeque<>();
    ArrayDeque<Short> mic3 = new ArrayDeque<>();
    ArrayDeque<Short> mic4 = new ArrayDeque<>();
    ArrayDeque<Short> mic5 = new ArrayDeque<>();
    ArrayDeque<Short> mic6 = new ArrayDeque<>();
    ArrayDeque<Short> mic7 = new ArrayDeque<>();

    private boolean inRead;

    public MicArray(Wishbone wb) {
        super(wb);
    }

    public void read (){
        if(inRead==false) {
            inRead = true;
            wb.SpiReadBurst((short) kMicrophoneArrayBaseAddress, data, 128 * 8 * 2);
            appendData();
            inRead = false;
        }else
            Log.w(TAG,"[MIC] skip read data!");
    }

    private class readData extends AsyncTask<Void,Void,Void>{
        @Override
        protected Void doInBackground(Void... voids) {
            appendData();
            return null;
        }
    }

    private void appendData(){
        for (int i=0;i<128;i++){
            mic0.add(ByteBuffer.wrap(data,(i*8+0)*2,2).order(ByteOrder.BIG_ENDIAN).getShort());
            mic1.add(ByteBuffer.wrap(data,(i*8+1)*2,2).order(ByteOrder.LITTLE_ENDIAN).getShort());
            mic2.add(ByteBuffer.wrap(data,(i*8+2)*2,2).order(ByteOrder.LITTLE_ENDIAN).getShort());
            mic3.add(ByteBuffer.wrap(data,(i*8+3)*2,2).order(ByteOrder.LITTLE_ENDIAN).getShort());
            mic4.add(ByteBuffer.wrap(data,(i*8+4)*2,2).order(ByteOrder.LITTLE_ENDIAN).getShort());
            mic5.add(ByteBuffer.wrap(data,(i*8+5)*2,2).order(ByteOrder.LITTLE_ENDIAN).getShort());
            mic6.add(ByteBuffer.wrap(data,(i*8+6)*2,2).order(ByteOrder.LITTLE_ENDIAN).getShort());
            mic7.add(ByteBuffer.wrap(data,(i*8+7)*2,2).order(ByteOrder.LITTLE_ENDIAN).getShort());
        }
    }

    public void clearData(){
    }

    public void sendDataToDebugIp(){
        // TODO: write to SD not work! maybe GT not support EXTERNALSTORAGE permission
        new sendData().execute(); // only for debugging, receive data with netcat
    }

    private class sendData extends AsyncTask<Void,Void,Void>{
        @Override
        protected Void doInBackground(Void... voids) {
            writeViaSocket();
            return null;
        }
    }

    private void writeViaSocket(){
        if(DEBUG)Log.d(TAG,"[MIC] write via socket..");
        if(DEBUG)Log.d(TAG,"[MIC] size: "+mic0.size());
        if(DEBUG)Log.d(TAG,"[MIC] data: "+mic0.toString());
        Socket socket = null;
        DataOutputStream dataOutputStream = null;
        DataInputStream dataInputStream = null;

        try {
            socket = new Socket(Config.EXTERNAL_DEBUG_IP, Config.EXTERNAL_DEBUG_PORT);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
            Iterator<Short> it = mic0.iterator();
            while (it.hasNext())
                dataOutputStream.writeShort(it.next());
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
            clearData();
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
