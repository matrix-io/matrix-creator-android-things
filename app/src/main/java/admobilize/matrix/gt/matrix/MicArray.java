package admobilize.matrix.gt.matrix;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;

import admobilize.matrix.gt.BoardDefaults;
import admobilize.matrix.gt.Config;

/**
 * Created by Antonio Vanegas @hpsaturn on 12/20/16.
 */

public class MicArray extends SensorBase {

    private static final String TAG = MicArray.class.getSimpleName();
    private static final boolean DEBUG = Config.DEBUG;

    private int current_mic =0;
    private int max_irq_samples;
    private int irq_samples;
    private boolean inRead;

    private byte[] data = new byte[128*8*2];
    private ArrayDeque<Short> mic0 = new ArrayDeque<>();
    private ArrayDeque<Short> mic1 = new ArrayDeque<>();
    private ArrayDeque<Short> mic2 = new ArrayDeque<>();
    private ArrayDeque<Short> mic3 = new ArrayDeque<>();
    private ArrayDeque<Short> mic4 = new ArrayDeque<>();
    private ArrayDeque<Short> mic5 = new ArrayDeque<>();
    private ArrayDeque<Short> mic6 = new ArrayDeque<>();
    private ArrayDeque<Short> mic7 = new ArrayDeque<>();

    private ArrayList<ArrayDeque>micarray=new ArrayList<>();
    private Gpio gpio;
    private OnMicArrayListener listener;
    private boolean continuous;


    public MicArray(Wishbone wb, PeripheralManagerService service) {
        super(wb);
        micarray.add(mic0);
        micarray.add(mic1);
        micarray.add(mic2);
        micarray.add(mic3);
        micarray.add(mic4);
        micarray.add(mic5);
        micarray.add(mic6);
        micarray.add(mic7);
        configMicDataInterrupt(service);
    }

    public interface OnMicArrayListener{
        void onCapture(int mic, ArrayDeque<Short>mic_data);
        void onCaptureAll(ArrayList<ArrayDeque>mic_array);
    }

    public void capture(int mic,int samples, boolean continuous, OnMicArrayListener listener ){
        this.current_mic=mic;
        this.max_irq_samples=samples;
        this.irq_samples=0;
        this.continuous=continuous;
        this.listener=listener;

        try {
            this.gpio.registerGpioCallback(onMicDataCallback);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configMicDataInterrupt(PeripheralManagerService service){
        try {
            gpio = service.openGpio(BoardDefaults.getGPIO_MIC_DATA());
            gpio.setDirection(Gpio.DIRECTION_IN);
            gpio.setActiveType(Gpio.ACTIVE_LOW);
            // Register for all state changes
            gpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private GpioCallback onMicDataCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            if(irq_samples<max_irq_samples){
                irq_samples++;
                read();
            }
            else if(irq_samples==max_irq_samples) {
                if(DEBUG)Log.i(TAG,"[MIC] "+max_irq_samples+" samples ready");
                listener.onCaptureAll(micarray);
                listener.onCapture(current_mic,micarray.get(current_mic));
                if(continuous)irq_samples=0; // START AGAIN
                else irq_samples=max_irq_samples+1; // STOP CALLBACK
            }
            return super.onGpioEdge(gpio);
        }
        @Override
        public void onGpioError(Gpio gpio, int error) {
            super.onGpioError(gpio, error);
            Log.w(TAG, "[MIC] onMicDataCallback error event: "+gpio + "==>" + error);
        }
    };


    private void read(){
        if(inRead==false) {
            inRead = true;
            wb.SpiReadBurst((short) kMicrophoneArrayBaseAddress, data, 128 * 8 * 2);
            appendData();
            inRead = false;
        }else
            Log.w(TAG,"[MIC] skip read data!");
    }

    private void appendData(){
        for (int i=0;i<128;i++){
            mic0.add(ByteBuffer.wrap(data,(i*8+0)*2,2).order(ByteOrder.BIG_ENDIAN).getShort());
            mic1.add(ByteBuffer.wrap(data,(i*8+1)*2,2).order(ByteOrder.BIG_ENDIAN).getShort());
            mic2.add(ByteBuffer.wrap(data,(i*8+2)*2,2).order(ByteOrder.BIG_ENDIAN).getShort());
            mic3.add(ByteBuffer.wrap(data,(i*8+3)*2,2).order(ByteOrder.BIG_ENDIAN).getShort());
            mic4.add(ByteBuffer.wrap(data,(i*8+4)*2,2).order(ByteOrder.BIG_ENDIAN).getShort());
            mic5.add(ByteBuffer.wrap(data,(i*8+5)*2,2).order(ByteOrder.BIG_ENDIAN).getShort());
            mic6.add(ByteBuffer.wrap(data,(i*8+6)*2,2).order(ByteOrder.BIG_ENDIAN).getShort());
            mic7.add(ByteBuffer.wrap(data,(i*8+7)*2,2).order(ByteOrder.BIG_ENDIAN).getShort());
        }
    }



    public void sendDataToDebugIp(int mic){
        // TODO: write to SD not work! GT not support EXTERNALSTORAGE permission
        new sendData(mic).execute(); // only for debugging, receive data with netcat
    }

    private class sendData extends AsyncTask<Void,Void,Void>{

        private final int mic;

        public sendData(int mic) {
            this.mic=mic;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            writeViaSocket(mic);
            return null;
        }
    }

    private void writeViaSocket(int mic){
        ArrayDeque current_mic = micarray.get(mic);
        if(DEBUG)Log.i(TAG, "[MIC] write via socket..");
        if(DEBUG)Log.i(TAG, "[MIC] ==> sending mic:"+mic+" size :"+current_mic.size());
        Socket socket = null;
        DataOutputStream dataOutputStream = null;
        DataInputStream dataInputStream = null;

        try {
            socket = new Socket(Config.EXTERNAL_DEBUG_IP, Config.EXTERNAL_DEBUG_PORT);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
            Iterator<Short> it = current_mic.iterator();
            while (it.hasNext())
                dataOutputStream.writeShort(it.next());
            if(DEBUG)Log.i(TAG, "[MIC] ==> output ready to "+Config.EXTERNAL_DEBUG_IP+":"+Config.EXTERNAL_DEBUG_PORT+" done.");
        } catch (UnknownHostException e) {
            if(DEBUG)Log.e(TAG, "[MIC] sending mic data error! "+e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            if(DEBUG)Log.e(TAG, "[MIC] sending mic data error! "+e.getMessage());
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

    public void clear() {
        Iterator<ArrayDeque> it = micarray.iterator();
        while (it.hasNext())it.next().clear();
    }

}
