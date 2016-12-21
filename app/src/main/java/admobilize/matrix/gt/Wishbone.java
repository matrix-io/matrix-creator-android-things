package admobilize.matrix.gt;

import android.util.Log;

import com.google.android.things.pio.SpiDevice;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Antonio Vanegas @hpsaturn on 12/19/16.
 */

public class Wishbone {

    private static final String TAG = Wishbone.class.getSimpleName();
    private static final boolean DEBUG = Config.DEBUG;

    private byte[] tx_buffer_ = new byte[4096];
    private byte[] rx_buffer_ = new byte[16];


    private final Lock _mutex = new ReentrantLock(true);
    private SpiDevice spiDevice;

    public Wishbone(SpiDevice spiDevice) {
        this.spiDevice = spiDevice;
    }

    // Full-duplex data transfer
    public void spiTransfer(SpiDevice device, byte[] tx_buffer) throws IOException {
        byte[] response = new byte[tx_buffer.length];
        device.transfer(tx_buffer, response, tx_buffer.length);
    }

    public void spiTransfer(SpiDevice device, byte[] tx_buffer, byte[] rx_buffer, int lenght) throws IOException {
        device.transfer(tx_buffer, rx_buffer, lenght);
    }

    public Boolean SpiWrite16(short add, byte[] data) {
        _mutex.lock();
        try {
            tx_buffer_[0] = WR0(add);
            tx_buffer_[1] = WR1(add, (byte) 0);
            tx_buffer_[2] = data[0];
            tx_buffer_[3] = data[1];
            spiTransfer(spiDevice,tx_buffer_);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            _mutex.unlock();
        }
    }

    public Boolean SpiWrite(short add, byte [] data, short inc) {
        _mutex.lock();
        try {
            tx_buffer_[0] = WR0(add);
            tx_buffer_[1] = WR1(add, inc);
            System.arraycopy(data,0,tx_buffer_,2,2);
//          memcpy(&tx_buffer_[2], data, 2);
            spiTransfer(spiDevice,tx_buffer_);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            _mutex.unlock();
        }
    }

    public Boolean SpiReadBurst(short add, byte [] data, int length) {
        _mutex.lock();
        try {
            tx_buffer_[0] = RD0(add);
            tx_buffer_[1] = RD1(add, (short) 1);
            spiTransfer(spiDevice, tx_buffer_,rx_buffer_,length+2);
            System.arraycopy(rx_buffer_,2,data,0,length);
//          memcpy(data, &rx_buffer_[2], length);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            _mutex.unlock();
        }
    }

    public Boolean SpiRead(short add, byte[] data, int length) {
        for (short w = 0; w < (length / 2); w++) {
//            if(DEBUG) Log.d(TAG,"spiRead word:"+w);
            if (!SpiRead16((short)(add + w),data, w)) return false;
        }
        return true;
    }

    public Boolean SpiRead16(short add, byte[] data, int inc) {
        _mutex.lock();
        try {
            int length = 2;
            tx_buffer_[0] = RD0(add);
            tx_buffer_[1] = RD1(add, (short) 0);
            spiTransfer(spiDevice,tx_buffer_,rx_buffer_,length+2);
            System.arraycopy(rx_buffer_,2,data,inc*2,length);
//            if(DEBUG) Log.d(TAG,"spiTransfer transmit:"+ Arrays.toString(tx_buffer_));
//            if(DEBUG) Log.d(TAG,"spiTransfer response:"+ Arrays.toString(rx_buffer_));
//            memcpy(data, &rx_buffer_[2], length);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            _mutex.unlock();
        }
    }

    private byte WR0 (short a){
        return (byte) ((a >> 6) & 0x0FF);
    }
    private byte WR1 (short a, short i){
        return (byte) (((a << 2) & 0xFC) | (i << 1));
    }
    private byte RD0(short a) {
        return (byte) ((a >> 6) & 0x0FF);
    }
    private byte RD1(short a, short i){
        return (byte) (((a << 2) & 0xFC) | 0x01 | (i << 1));
    }

}
