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

import com.google.android.things.pio.PeripheralManager;
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
import admobilize.matrix.gt.matrix.MicArrayDriver;
import admobilize.matrix.gt.matrix.Pressure;
import admobilize.matrix.gt.matrix.UV;
import admobilize.matrix.gt.matrix.Wishbone;

/**
 * Sample usage of the Matrix-Creator sensors and GPIO calls
 *
 * REQUIREMENTS:
 *
 * - MatrixCreator Google Things image on RaspberryPi3
 * - MATRIXCreator or MATRIXVoice Hat (see README.md file)
 *
 * Created by Antonio Vanegas @hpsaturn on 12/19/16.
 *
 *  rev20170817 refactor for MATRIXVoice hat
 *  rev20170901 micarray working, all mics
 *  rev20170908 mics energy on Everloop, support two boards
 */

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final boolean DEBUG = Config.DEBUG;

    private static final boolean ENABLE_EVERLOOP_PROGRESS  = true;
    private static final boolean ENABLE_DRAW_MICS          = true && !ENABLE_EVERLOOP_PROGRESS;
    private static final boolean ENABLE_LOG_SENSORS        = true && Config.MATRIX_CREATOR;
    private static final boolean ENABLE_MICARRAY_RECORD    = false; // true => 1024 samples ~8 sec
    private static final boolean ENABLE_CONTINOUNS_CAPTURE = !ENABLE_MICARRAY_RECORD;
    private static final int     INTERVAL_POLLING_MS       = 1000;

    private Handler mHandler = new Handler();
    private SpiDevice spiDevice;
    private Wishbone wb;
    private Everloop everloop;
    private MicArray micArray;
    private Pressure pressure;
    private Humidity humidity;
    private IMU imuSensor;
    private UV uvSensor;
    private MicArrayDriver mMicArrayDriver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Starting Matrix-Creator device config..");

        PeripheralManager service = PeripheralManager.getInstance();
        while(!configSPI(service)){
            Log.i(TAG, "waiting for SPI..");
        }
        wb=new Wishbone(spiDevice);
        initDevices(service);
        mHandler.post(mPollingRunnable);
    }

    private void initDevices(PeripheralManager service) {
        pressure = new Pressure(wb);
        humidity = new Humidity(wb);
        imuSensor = new IMU(wb);
        uvSensor = new UV(wb);

        // TODO: autodetection of hat via SPI register
        everloop = new Everloop(wb); // NOTE: please change to right board
        everloop.clear();
        everloop.write();

        micArray = new MicArray(wb);
        Log.d(TAG,"[MIC] starting capture..");
        int samples = 2;
        if(ENABLE_MICARRAY_RECORD) samples=1024;
        micArray.capture(7, samples, ENABLE_CONTINOUNS_CAPTURE, onMicArrayListener);
        mMicArrayDriver = new MicArrayDriver(micArray);
        mMicArrayDriver.registerAudioInputDriver();
        mMicArrayDriver.startRecording();
    }

    private boolean configSPI(PeripheralManager service){
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
//                spiDevice.setBitJustification(false); // MSB first
                return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API (SPI)..");
        }

        return false;
    }


    private MicArray.OnMicArrayListener onMicArrayListener =  new MicArray.OnMicArrayListener() {
        @Override
        public void onCapture(int mic, ArrayDeque<Short> mic_data) {
//            Log.d(TAG, "[MIC] mic:"+mic+" size :"+mic_data.size());
//            Log.d(TAG, "[MIC] mic:"+mic+" data :"+mic_data.toString());
            // TODO: write to SD not work! GT not support EXTERNALSTORAGE permission
            if(ENABLE_MICARRAY_RECORD)micArray.sendDataToDebugIp(mic);
        }

        @Override
        public void onCaptureAll(ArrayList<ArrayDeque> mic_array) {
//            if(DEBUG)Log.d(TAG, "[MIC] all mics data size:");
            Iterator<ArrayDeque> it = mic_array.iterator();
            int mic=0;
            ArrayList<Short>micArrayEnergy=new ArrayList<>();
            while (it.hasNext()){
                short energy = getEnergy(it.next());
                micArrayEnergy.add(energy);
//                if(DEBUG)Log.d(TAG, "[MIC] mic:"+mic+++" energy "+energy);
            }
            if(ENABLE_DRAW_MICS) {
                everloop.drawMicArrayEnergy(micArrayEnergy);
                everloop.write();
            }
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
            }

            counter++;

            if(counter==5)mMicArrayDriver.stopRecording();
            if(counter==10)mMicArrayDriver.saveRecording();

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
            mMicArrayDriver.close();
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        } catch (Exception e) {
            Log.e(TAG, "Error on MicArrayDriver close", e);
        }
    }

}
