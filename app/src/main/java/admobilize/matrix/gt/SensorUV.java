package admobilize.matrix.gt;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Created by Antonio Vanegas @hpsaturn on 12/20/16.
 */

public class SensorUV {

    private Wishbone wb;

    public SensorUV(Wishbone wb) {
        this.wb=wb;
    }

    public float read (){
        byte[] data = new byte[8];
        wb.SpiRead((short) (0x3800+(0x00 >> 1)),data,4);
        return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

}
