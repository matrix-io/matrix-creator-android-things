package admobilize.matrix.gt.matrix;

import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.userdriver.AudioInputDriver;
import com.google.android.things.userdriver.UserDriverManager;

import java.io.IOException;
import java.nio.ByteBuffer;

import admobilize.matrix.gt.Config;


/**
 * Created by Antonio Vanegas @hpsaturn on 9/9/17.
 */

public class MicArrayDriver implements AutoCloseable {

    private static final String TAG = MicArrayDriver.class.getSimpleName();
    private static final boolean DEBUG = Config.DEBUG;

    // buffer of 0.05 sec of sample data at 48khz / 16bit.
    private static final int BUFFER_SIZE = 96000 / 20;
    // buffer of 0.5 sec of sample data at 48khz / 16bit.
    private static final int FLUSH_SIZE = 48000;
    private static final int SAMPLE_BLOCK_SIZE = 128;

    private AudioRecord mAudioRecord = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(AUDIO_FORMAT_IN_MONO)
                .setBufferSizeInBytes(BUFFER_SIZE)
                .build();

    private MicArray micArray;
    private Gpio mTriggerGpio;
    private AudioInputUserDriver mAudioInputDriver;

    // Audio constants.
    private static final String PREF_CURRENT_VOLUME = "current_volume";
    private static final int SAMPLE_RATE = 16000;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int DEFAULT_VOLUME = 100;

    private static final AudioFormat AUDIO_FORMAT_STEREO =
            new AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                    .setEncoding(ENCODING)
                    .setSampleRate(SAMPLE_RATE)
                    .build();

    private static final AudioFormat AUDIO_FORMAT_IN_MONO =
            new AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .setEncoding(ENCODING)
                    .setSampleRate(SAMPLE_RATE)
                    .build();
    private HandlerThread mAssistantThread;
    private Handler mAssistantHandler;

    public MicArrayDriver(MicArray micArray) {
        this.micArray=micArray;
    }

    @Override
    public void close() throws Exception {
        unregisterAudioInputDriver();
    }

    private class AudioInputUserDriver extends AudioInputDriver {
        @Override
        public void onStandbyChanged(boolean b) {
        }

        @Override
        public int read(ByteBuffer byteBuffer, int i) {
            try {
                return micArray.read(byteBuffer, i);
            } catch (IOException e) {
                Log.e(TAG, "[MIC] error during read operation:", e);
                return -1;
            }
        }
    }

    public void registerAudioInputDriver() {
        mAudioInputDriver = new AudioInputUserDriver();
        UserDriverManager.getManager().registerAudioInputDriver(mAudioInputDriver, AUDIO_FORMAT_IN_MONO,
                AudioDeviceInfo.TYPE_BUILTIN_MIC, BUFFER_SIZE);
    }

    public void unregisterAudioInputDriver() {
        if (mAudioInputDriver != null) {
            UserDriverManager.getManager().unregisterAudioInputDriver(mAudioInputDriver);
            mAudioInputDriver = null;
        }
    }

    public void startRecording() {
        // Start recording audio.
        Log.d(TAG, "[MIC] startRecording..");
        mAssistantThread = new HandlerThread("assistantThread");
        mAssistantThread.start();
        mAssistantHandler = new Handler(mAssistantThread.getLooper());
        mAssistantHandler.post(mStreamAssistantRequest);
    }

    private Runnable mStreamAssistantRequest = new Runnable() {
        @Override
        public void run() {
            ByteBuffer audioData = ByteBuffer.allocateDirect(SAMPLE_BLOCK_SIZE);
            int result = mAudioRecord.read(audioData, audioData.capacity(), AudioRecord.READ_BLOCKING);
            if (result < 0) {
                Log.e(TAG, "error reading from audio stream:" + result);
                return;
            }
            Log.d(TAG, "streaming ConverseRequest: " + result);
            Log.d(TAG, "[MIC] result: "+result);
            Log.d(TAG, "[MIC] audioData: "+audioData.capacity());
            Log.d(TAG, "[MIC] audioData: "+audioData.array());
        }
    };


}
