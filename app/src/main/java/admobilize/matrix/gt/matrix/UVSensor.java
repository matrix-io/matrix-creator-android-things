package admobilize.matrix.gt.matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by Antonio Vanegas @hpsaturn on 12/20/16.
 */

public class UVSensor extends SensorBase {

    public UVSensor(Wishbone wb) {
        super(wb);
    }

    public float read (){
        byte[] data = new byte[8];
        wb.SpiRead((short) (kMCUBaseAddress+(kMemoryOffsetUV >> 1)),data,4);
        return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

}
