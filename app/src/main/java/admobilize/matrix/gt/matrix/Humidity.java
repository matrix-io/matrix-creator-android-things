package admobilize.matrix.gt.matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import admobilize.matrix.gt.Config;

/**
 * Created by Antonio Vanegas @hpsaturn on 12/20/16.
 */

public class Humidity extends SensorBase {

    private static final String TAG = Humidity.class.getSimpleName();
    private static final boolean DEBUG = Config.DEBUG;

    private float humidity;
    private float temperature;

    public Humidity(Wishbone wb) {
        super(wb);
    }

    public void read (){
        byte[] data = new byte[8];
        wb.SpiRead((short) (kMCUBaseAddress+(kMemoryOffsetHumidity >> 1)),data,8);
        this.humidity =ByteBuffer.wrap(data,0,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        this.temperature=ByteBuffer.wrap(data,4,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    public float getHumidity() {
        return humidity;
    }

    public float getTemperature() {
        return temperature;
    }
}
