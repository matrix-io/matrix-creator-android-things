package admobilize.matrix.gt.matrix;

/**
 * Created by Antonio Vanegas @hpsaturn on 12/20/16.
 */

public abstract class SensorBase {


    public Wishbone wb;

    public SensorBase(Wishbone wb) {
        this.wb = wb;
    }

    // FPGA Wishbone address map
    public int kMicrophoneArrayBaseAddress = 0x1800;
    public int kEverloopBaseAddress = 0x3000;
    public int kGPIOBaseAddress = 0x2800;
    public int kMCUBaseAddress = 0x3800;

    // MCU offsets map
    public int kMemoryOffsetUV = 0x00;
    public int kMemoryOffsetPressure = 0x10;
    public int kMemoryOffsetHumidity = 0x20;
    public int kMemoryOffsetIMU = 0x30;
    public int kMemoryOffsetMCU = 0x90;

}
