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

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import admobilize.matrix.gt.matrix.Everloop;
import admobilize.matrix.gt.matrix.Humidity;
import admobilize.matrix.gt.matrix.IMU;
import admobilize.matrix.gt.matrix.MicArray;
import admobilize.matrix.gt.matrix.Pressure;
import admobilize.matrix.gt.matrix.UV;
import admobilize.matrix.gt.matrix.Wishbone;

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
 *  rev20170817 refactor for MATRIXVoice hat
 *  rev20170901 micarray working, all mics
 */

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final boolean DEBUG = Config.DEBUG;

    private static final boolean ENABLE_EVERLOOP_PROGRESS = false;
    private static final boolean ENABLE_LOG_SENSORS       = false;
    private static final boolean ENABLE_MICARRAY_DEBUG    = false;
    private static final int     INTERVAL_POLLING_MS      = 1000;

    private Handler mHandler = new Handler();
    private SpiDevice spiDevice;
    private Wishbone wb;
    private Everloop everloop;
    private MicArray micArray;
    private Pressure pressure;
    private Humidity humidity;
    private IMU imuSensor;
    private UV uvSensor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Starting Matrix-Creator device config..");

        PeripheralManagerService service = new PeripheralManagerService();
        while(!configSPI(service)){
            Log.d(TAG, "waiting for SPI..");
        }
        wb=new Wishbone(spiDevice);
        initDevices(service);
        mHandler.post(mPollingRunnable);
    }

    private void initDevices(PeripheralManagerService service) {
        pressure = new Pressure(wb);
        humidity = new Humidity(wb);
        imuSensor = new IMU(wb);
        uvSensor = new UV(wb);

        // TODO: autodetection of hat via SPI register
        everloop = new Everloop(wb,Everloop.MATRIX_CREATOR); // NOTE: please change to right board
        everloop.clear();
        everloop.write();

        micArray = new MicArray(wb,service);
        Log.d(TAG,"[MIC] starting capture..");
        micArray.capture(7,4,true,onMicArrayListener);
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
                spiDevice.setBitsPerWord(8);          // 8 BP
                spiDevice.setBitJustification(false); // MSB first
                return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API (SPI)", e);
            e.printStackTrace();
        }

        return false;
    }

    private short max_energy=0;
    private MicArray.OnMicArrayListener onMicArrayListener =  new MicArray.OnMicArrayListener() {
        @Override
        public void onCapture(int mic, ArrayDeque<Short> mic_data) {
//            Log.d(TAG, "[MIC] mic:"+mic+" size :"+mic_data.size());
//            Log.d(TAG, "[MIC] mic:"+mic+" data :"+mic_data.toString());

            // TODO: write to SD not work! GT not support EXTERNALSTORAGE permission
            if(ENABLE_MICARRAY_DEBUG)micArray.sendDataToDebugIp(mic);
            else micArray.clear();
        }

        @Override
        public void onCaptureAll(ArrayList<ArrayDeque> mic_array) {
            if(DEBUG)Log.d(TAG, "[MIC] all mics data size:");
            Iterator<ArrayDeque> it = mic_array.iterator();
            int mic=0;
            ArrayList<Short>micArrayEnergy=new ArrayList<>();
            while (it.hasNext()){
                short energy = getEnergy(it.next());
                micArrayEnergy.add(energy);
                if(DEBUG)Log.d(TAG, "[MIC] mic:"+mic+++" energy "+energy);
            }
            everloop.drawMicArrayEnergy(micArrayEnergy);
            everloop.write();
        }
    };

    private short getEnergy(ArrayDeque<Short> mic) {
        Iterator<Short> it = mic.iterator();

        short energy=0;
        while(it.hasNext()){
            short val = it.next();
            if(val>=0)
            energy = (short) (energy+(val*val));
        }
        return (short) Math.abs(energy);
    }


    private Runnable mPollingRunnable = new Runnable() {

        private long counter=0;
        @Override
        public void run() {
            // Exit Runnable if devices is already closed
            if (wb == null) return;
            if(ENABLE_LOG_SENSORS) {
                String output;
                // Read UVsensor
                output = "UV: " + uvSensor.read() + "\t";
                // Read Pressure device values
                pressure.read();
                output = output + "AL: " + pressure.getAltitude() + "\t";
                output = output + "PR: " + pressure.getPressure() + "\t";
                output = output + "TP: " + pressure.getTemperature() + "\t";
                // Read Humidity device values
                humidity.read();
                output = output + "HM: " + humidity.getHumidity() + "\t";
                output = output + "TP: " + humidity.getTemperature() + "\t";
                // Read IMU device values
                imuSensor.read();
                output = output + "YW: " + imuSensor.getYaw() + "\t";
                output = output + "PT: " + imuSensor.getPitch() + "\t";
                output = output + "RL: " + imuSensor.getRoll() + "\t";
                Log.d(TAG,output);
            }

            if(ENABLE_EVERLOOP_PROGRESS) {
                everloop.drawProgress((int) counter);
                everloop.write();
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
            everloop.clear();
            everloop.write();
            spiDevice.close();
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }
    }

}
