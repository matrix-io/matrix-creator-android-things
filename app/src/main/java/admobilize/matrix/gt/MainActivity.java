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

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import admobilize.matrix.gt.matrix.Everloop;
import admobilize.matrix.gt.matrix.MicArray;
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
        wb=new Wishbone(spiDevice);
        initDevices(spiDevice,service);
//        mHandler.post(mPollingRunnable);
    }

    private void initDevices(SpiDevice spiDevice,PeripheralManagerService service) {
        everloop = new Everloop(wb);
        everloop.clear();
        everloop.write(everloop.ledImage);
        micArray = new MicArray(wb,service);
        Log.d(TAG,"[MIC] starting capture..");
        micArray.capture(7,1024,onMicArrayListener);
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

    private MicArray.OnMicArrayListener onMicArrayListener =  new MicArray.OnMicArrayListener() {
        @Override
        public void onCapture(int mic, ArrayDeque<Short> mic_data) {
            Log.d(TAG, "[MIC] :"+mic+" data: "+mic_data.toString());
            micArray.sendDataToDebugIp(mic);
        }

        @Override
        public void onCaptureAll(ArrayList<ArrayDeque> mic_array) {
            Log.d(TAG, "[MIC] buffers: "+mic_array.toString());
        }
    };

    private Runnable mPollingRunnable = new Runnable() {

        private long counter=0;
        @Override
        public void run() {
            // Exit Runnable if devices is already closed
            if (wb == null) return;
            if(SHOW_EVERLOOP_PROGRESS) {
                everloop.drawProgress((int) counter);
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
