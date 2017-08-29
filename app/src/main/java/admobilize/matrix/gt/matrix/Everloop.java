package admobilize.matrix.gt.matrix;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Antonio Vanegas @hpsaturn on 12/21/16.
 */

public class Everloop extends SensorBase {

    public ArrayList<Everloop.LedValue>ledImage=new ArrayList<>();

    public Everloop(Wishbone wb) {
        super(wb);
        init();
    }

    public boolean write(ArrayList<LedValue> ledImage) {
        if (wb==null) return false;
        LedValue led;
        byte[] wb_data_buffer = new byte[2];
        int addr_offset = 0;

        Iterator<LedValue> it = ledImage.iterator();
        while(it.hasNext()){
            led=it.next();
            wb_data_buffer[0] = led.green;
            wb_data_buffer[1] = led.red;
            wb.SpiWrite((short)(kEverloopBaseAddress + addr_offset), wb_data_buffer, (short) 0);
            wb_data_buffer[0] = led.blue;
            wb_data_buffer[1] = led.white;
            wb.SpiWrite((short)(kEverloopBaseAddress + addr_offset+1), wb_data_buffer, (short) 0);
            addr_offset = addr_offset + 2;
        }
        this.ledImage=ledImage;
        return true;
    }

    public void clear(){
        LedValue led;
        Iterator<LedValue> it = ledImage.iterator();
        while(it.hasNext()){
            led = it.next();
            led.red=0;
            led.green=0;
            led.blue=0;
            led.white=0;
        }
    }

    public void init(){
        for(int i=0;i<18;i++){
            ledImage.add(new LedValue());
        }
    }

    public static class LedValue {
        public byte red;
        public byte green;
        public byte blue;
        public byte white;
    }

}
