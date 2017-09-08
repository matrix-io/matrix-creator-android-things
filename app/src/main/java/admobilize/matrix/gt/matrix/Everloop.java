package admobilize.matrix.gt.matrix;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Antonio Vanegas @hpsaturn on 12/21/16.
 */

public class Everloop extends SensorBase {

    public ArrayList<Everloop.LedValue> ledImage = new ArrayList<>();
    private boolean toggleColor;
    private int led_count=35;
    public static int MATRIX_CREATOR = 0;
    public static int MATRIX_VOICE = 1;

    public Everloop(Wishbone wb,int matrix_board_type) {
        super(wb);
        if(matrix_board_type==MATRIX_VOICE)led_count=18;
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

    public void setColor(int pos, int r, int g, int b, int w) {
        ledImage.get(pos % led_count).red   = (byte) r;
        ledImage.get(pos % led_count).green = (byte) g;
        ledImage.get(pos % led_count).blue  = (byte) b;
        ledImage.get(pos % led_count).white = (byte) w;
    }

    public void drawProgress(int counter) {
        if(counter % led_count ==0) toggleColor=!toggleColor;
        int min = counter % led_count;
        int solid = led_count;
        for (int i = 0; i <= min; i++) {
            if(toggleColor) setColor(i, i/3, solid/5, 0, 0);
            else setColor(i, solid/5, i/3, 0, 0);
            solid=led_count-i;
        }
    }

    public void init(){
        for(int i=0;i<led_count;i++){
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
