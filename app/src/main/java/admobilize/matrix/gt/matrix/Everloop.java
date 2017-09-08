package admobilize.matrix.gt.matrix;

import java.util.ArrayList;
import java.util.Iterator;

import admobilize.matrix.gt.Config;

/**
 * Created by Antonio Vanegas @hpsaturn on 12/21/16.
 */

public class Everloop extends SensorBase {

    public ArrayList<Everloop.LedValue> ledImage = new ArrayList<>();
    private boolean toggleColor;
    private int led_count=35;

    public Everloop(Wishbone wb) {
        super(wb);
        if(!Config.MATRIX_CREATOR){
            this.led_count=18;
        }
        init();
    }

    public boolean write() {
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

    public void drawMicArrayEnergy(ArrayList<Short> mic_array_energy){
        ledImage.clear();
        if(Config.MATRIX_CREATOR) {
            Iterator<Short> it = mic_array_energy.iterator();
            int mic = 0;
            while (it.hasNext()) {
                addMicSeperator();
                addMicSegment(it.next()); // mic energy
                if (mic == 2 && Config.MATRIX_CREATOR) addMicSeperator();
                if (mic == 5 && Config.MATRIX_CREATOR) addMicSeperator();
                mic++;
            }
            if (Config.MATRIX_CREATOR) addMicSeperator();
        }
        else{
            addMicSegment(mic_array_energy.get(1));
            addMicSeperator();
            addMicSegment(mic_array_energy.get(2));
            addMicSeperator();
            addMicSeperator();
            addMicSegment(mic_array_energy.get(3));
            addMicSeperator();
            addMicSegment(mic_array_energy.get(4));
            addMicSeperator();
            addMicSeperator();
            addMicSegment(mic_array_energy.get(5));
            addMicSeperator();
            addMicSeperator();
            addMicSegment(mic_array_energy.get(6));
            addMicSeperator();
            addMicSegment(mic_array_energy.get(7));
            addMicSeperator();
            addMicSegment(mic_array_energy.get(0));
        }
    }

    private void addMicSegment(short m0) {
        int step=3;
        if(!Config.MATRIX_CREATOR)step=1;
        for (int i = 0; i<step; i++){
            LedValue ledM0 = new LedValue();
            if(100<m0 && m0<8192)
                ledM0 = new LedValue(getLedValueFromEnergy(m0)/3, 4, 0, 0);
            if(8192<m0 && m0<16384)
                ledM0 = new LedValue(getLedValueFromEnergy(m0)/2, 4, 0, 0);
            if(16384<m0 && m0<24576)
                ledM0 = new LedValue(getLedValueFromEnergy(m0), 4, 0, 0);
            if(24576<m0 && m0<32768)
                ledM0 = new LedValue(getLedValueFromEnergy(m0), 0, 0, 0);
            ledImage.add(ledM0);
        }
    }

    private void addMicSeperator(){
        ledImage.add(new LedValue(0, 2, 0, 0)); // seperator
    }

    private short getLedValueFromEnergy(short m0){
        return (short) ((m0*255)/32768);
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

        public LedValue() {
        }

        public LedValue(byte red, byte green, byte blue, byte white) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.white = white;
        }

        public LedValue(int red, int green, int blue, int white) {
            this.red = (byte) red;
            this.green = (byte) green;
            this.blue = (byte) blue;
            this.white = (byte) white;
        }
    }

}
