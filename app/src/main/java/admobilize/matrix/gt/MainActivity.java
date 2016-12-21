/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package admobilize.matrix.gt;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;
import java.util.List;

import admobilize.matrix.gt.matrix.Humidity;
import admobilize.matrix.gt.matrix.Pressure;
import admobilize.matrix.gt.matrix.UV;
import admobilize.matrix.gt.matrix.Wishbone;

/**
 * Sample usage of the Matrix-Creator sensors and GPIO calls
 *
 * REQUIREMENTS:
 *
 * - MatrixCreator Google Things image on RaspberryPi3
 * - MatrixCreator hat
 *
 * Created by Antonio Vanegas @hpsaturn on 12/19/16.
 */

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final boolean DEBUG = Config.DEBUG;

    private static final int INTERVAL_POLLING_MS = 1000;

    private Handler mHandler = new Handler();
    private Gpio mLedGpio;
    private SpiDevice spiDevice;
    private Wishbone wb;
    private UV uvSensor;
    private Pressure pressure;
    private Humidity humidity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG)Log.i(TAG, "Starting Matrix-Creator device config..");
        PeripheralManagerService service = new PeripheralManagerService();
        configSPI(service);
        configGPIO(service);
        // Runnable that continuously update sensors and LED (Matrix LED on GPIO21)
        mHandler.post(mPollingRunnable);
    }

    private void configGPIO(PeripheralManagerService service){
        try {
            String pinName = BoardDefaults.getGPIOForLED();
            mLedGpio = service.openGpio(pinName);
            mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            Log.i(TAG, "Start blinking LED GPIO pin");
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }
    }

    private void configSPI(PeripheralManagerService service){
        try {
            List<String> deviceList = service.getSpiBusList();
            if (deviceList.isEmpty()) {
                Log.i(TAG, "No SPI bus available on this device.");
            } else {
                Log.i(TAG, "List of available devices: " + deviceList);
            }
            spiDevice = service.openSpiDevice(BoardDefaults.getSpiBus());
            spiDevice.setMode(SpiDevice.MODE3);
            spiDevice.setFrequency(18000000);     // 18MHz
            spiDevice.setBitsPerWord(8);          // 8 BPW
            spiDevice.setBitJustification(false); // MSB first

            wb=new Wishbone(spiDevice);
            uvSensor = new UV(wb);
            pressure = new Pressure(wb);
            humidity = new Humidity(wb);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Runnable mPollingRunnable = new Runnable() {
        @Override
        public void run() {
            // Exit Runnable if devices is already closed
            if (mLedGpio == null || wb == null) return;
            try {
                mLedGpio.setValue(!mLedGpio.getValue());
                String output;
                // Read UVsensor
                output="UV: "+ uvSensor.read()+"\t";
                // Read Pressure device values
                pressure.read();
                output=output+"ALT: "+ pressure.getAltitude()+"\t";
                output=output+"PRS: "+ pressure.getPressure()+"\t";
                output=output+"TEM: "+ pressure.getTemperature()+"\t";
                // Read Humidity device values
                humidity.read();
                output=output+"HUM: "+ humidity.getHumidity()+"\t";
                output=output+"TEM: "+ humidity.getTemperature()+"\t";

                if(DEBUG)Log.i(TAG,output);

                // Reschedule the same runnable in {#INTERVAL_POLLING_MS} milliseconds
                mHandler.postDelayed(mPollingRunnable, INTERVAL_POLLING_MS);

            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove pending polling Runnable from the handler.
        mHandler.removeCallbacks(mPollingRunnable);
        if(DEBUG)Log.i(TAG, "Closing devices and GPIO");
        try {
            mLedGpio.close();
            spiDevice.close();
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        } finally {
            mLedGpio = null;
            spiDevice = null;
        }
    }

}
