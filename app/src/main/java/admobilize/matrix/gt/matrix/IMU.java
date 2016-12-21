package admobilize.matrix.gt.matrix;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import admobilize.matrix.gt.Config;

/**
 * Created by Antonio Vanegas @hpsaturn on 12/20/16.
 */

public class IMU extends SensorBase {

    private static final String TAG = IMU.class.getSimpleName();
    private static final boolean DEBUG = Config.DEBUG;

    private float yaw;
    private float pitch;
    private float roll;

    private float ax;  // acceleration
    private float ay;
    private float az;

    private float gx;  // gyroscope
    private float gy;
    private float gz;

    private float mx; // magnetometer
    private float my;
    private float mz;


    public IMU(Wishbone wb) {
        super(wb);
    }

    public void read (){
        byte[] data = new byte[48];
        wb.SpiRead((short) (kMCUBaseAddress+(kMemoryOffsetIMU >> 1)),data,48);
        this.yaw =ByteBuffer.wrap(data,0,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        this.pitch =ByteBuffer.wrap(data,4,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        this.roll =ByteBuffer.wrap(data,8,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        this.ax=ByteBuffer.wrap(data,12,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        this.ay=ByteBuffer.wrap(data,16,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        this.az=ByteBuffer.wrap(data,20,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        this.gx=ByteBuffer.wrap(data,24,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        this.gy=ByteBuffer.wrap(data,28,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        this.gz=ByteBuffer.wrap(data,32,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        this.mx=ByteBuffer.wrap(data,36,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        this.my=ByteBuffer.wrap(data,40,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        this.mz=ByteBuffer.wrap(data,44,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public float getRoll() {
        return roll;
    }

    public float getAx() {
        return ax;
    }

    public float getAy() {
        return ay;
    }

    public float getAz() {
        return az;
    }

    public float getGx() {
        return gx;
    }

    public float getGy() {
        return gy;
    }

    public float getGz() {
        return gz;
    }

    public float getMx() {
        return mx;
    }

    public float getMy() {
        return my;
    }

    public float getMz() {
        return mz;
    }

}
