/*
 * Copyright 2016 <Admobilize>
 * MATRIX Labs  [http://creator.matrix.one]
 * This file is part of MATRIX Creator Google Things (GT)
 *
 * MATRIX Creator GT is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package admobilize.matrix.gt;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import admobilize.matrix.gt.matrix.Everloop;
import admobilize.matrix.gt.matrix.MicArray;
import admobilize.matrix.gt.matrix.Wishbone;

import static admobilize.matrix.gt.matrix.Everloop.LedValue;

/**
 * Sample usage of the Matrix-Creator sensors and GPIO calls
 *
 * REQUIREMENTS:
 *
 * - MatrixCreator Google Things image on RaspberryPi3
 * - MATRIXVoice Hat
 *
 * Created by Antonio Vanegas @hpsaturn on 12/19/16.
 *
 *  rev20170817 refactor for Voice hat
 */


public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final boolean DEBUG = Config.DEBUG;

    private boolean SHOW_EVERLOOP_PROGRESS = true;
    private static final int INTERVAL_POLLING_MS = 10;

    private Handler mHandler = new Handler();
    private SpiDevice spiDevice;
    private Wishbone wb;
    private Everloop everloop;
    private boolean toggleColor;
    private MicArray micArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Starting Matrix-Creator device config..");

        // We need permission to access the camera
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "No permission for write on external storage!");
        }

        PeripheralManagerService service = new PeripheralManagerService();
        while(!configSPI(service)){
            Log.d(TAG, "waiting for SPI..");
        }
        initDevices(spiDevice);
        configMicDataInterrupt(service);
        Log.d(TAG,"[MIC] starting capture..");
//        mHandler.post(mPollingRunnable);
    }

    private void initDevices(SpiDevice spiDevice) {
        wb=new Wishbone(spiDevice);
        micArray = new MicArray(wb);
        everloop = new Everloop(wb);
        everloop.clear();
        everloop.write(everloop.ledImage);
    }

    private boolean configSPI(PeripheralManagerService service){
        try {
            List<String> deviceList = service.getSpiBusList();
            if (deviceList.isEmpty()) {
                Log.i(TAG, "No SPI bus available");
            } else {
                Log.i(TAG, "List of available devices: " + deviceList);
                spiDevice = service.openSpiDevice(BoardDefaults.getSpiBus());
                spiDevice.setMode(SpiDevice.MODE3);
                spiDevice.setFrequency(18000000);     // 18MHz
                spiDevice.setBitsPerWord(8);          // 8 BPW
                spiDevice.setBitJustification(false); // MSB first
                return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API (SPI)", e);
            e.printStackTrace();
        }

        return false;
    }

    public void configMicDataInterrupt(PeripheralManagerService service){
        try {
            Gpio gpio = service.openGpio(BoardDefaults.getGPIO_MIC_DATA());
            gpio.setDirection(Gpio.DIRECTION_IN);
            gpio.setActiveType(Gpio.ACTIVE_LOW);
            // Register for all state changes
            gpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            gpio.registerGpioCallback(onMicDataCallback);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    int max_irq_samples=1024;
    int irq_samples=0;
    private boolean send_data;

    private GpioCallback onMicDataCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            if(irq_samples<max_irq_samples){
                irq_samples++;
                micArray.read();
            }
            else if(!send_data) {
                Log.i(TAG,"[MIC] "+max_irq_samples+" samples");
                micArray.sendDataToDebugIp();
//                micArray.clearData();
                send_data=true;
//                irq_samples=0;
            }
            return super.onGpioEdge(gpio);
        }
        @Override
        public void onGpioError(Gpio gpio, int error) {
            super.onGpioError(gpio, error);
            Log.w(TAG, "[MIC] onMicDataCallback error event: "+gpio + "==>" + error);
        }
    };

    void setColor(ArrayList<LedValue>leds, int pos, int r, int g, int b, int w) {
        leds.get(pos % 18).red   = (byte) r;
        leds.get(pos % 18).green = (byte) g;
        leds.get(pos % 18).blue  = (byte) b;
        leds.get(pos % 18).white = (byte) w;
    }

    void drawProgress(ArrayList<LedValue>leds, int counter) {
        if(counter % 18 ==0) toggleColor=!toggleColor;
        int min = counter % 18;
        int solid = 18;
        for (int i = 0; i <= min; i++) {
            if(toggleColor) setColor(leds, i, i/3, solid/5, 0, 0);
            else setColor(leds, i, solid/5, i/3, 0, 0);
            solid=18-i;
        }
    }

    private Runnable mPollingRunnable = new Runnable() {

        private long counter=0;
        @Override
        public void run() {
            // Exit Runnable if devices is already closed
            if (wb == null) return;
            if(SHOW_EVERLOOP_PROGRESS) {
                drawProgress(everloop.ledImage, (int) counter);
                everloop.write(everloop.ledImage);
                counter++;
            }
            // Reschedule the same runnable in {#INTERVAL_POLLING_MS} milliseconds
            mHandler.postDelayed(mPollingRunnable, INTERVAL_POLLING_MS);

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove pending polling Runnable from the handler.
        mHandler.removeCallbacks(mPollingRunnable);
        if(DEBUG)Log.i(TAG, "Closing devices and GPIO");
        try {
            SHOW_EVERLOOP_PROGRESS=false;
            everloop.clear();
            everloop.write(everloop.ledImage);
            spiDevice.close();
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }
    }

}
