Matrix Creator-Android Things (ALPHA)
=====================================

This Android Things app runs basic tests for Matrix sensors and Everloop ring. 

**IMPORTANT**: Please, note that these sample are not necessarily the easiest way because
the Google Things source code and documentation has not been published or completed. The next 
documentation is a Alpha version, for now, **PLEASE** support us with vote documentation request issue 
in this link: [issue #2 simplepio repository](https://github.com/androidthings/sample-simplepio/issues/2)

Pre-requisites
--------------

- RaspberryPi 3
- Matrix Creator
- Android Studio 2.2+

#### Firmware installation

For now you can test Matrix Creator with Google Things, for this we need FPGA burner running from root privileges, for it please follow next steps:

On your pc:

1. Flashing rpi3 Google Things image and connect with it via adb. [more info](https://developer.android.com/things/hardware/raspberrypi.html#flashing_the_image)
2. Obtain root privileges: `adb root` (take some seconds)
3. Mount partions on write mode `adb remount`
4. Clone repository and submodules: 

    ```bash
    git clone --recursive https://github.com/matrix-io/matrix-creator-android-things.git`
    ```
5. Copy firmware, burner, flashing script, and sensors test:

    ```bash
    cd matrix-creator-android-things/firmware
    adb push matrix_system.bit /system/bin/
    adb push matrix-xc3sprog /system/bin/
    adb push matrix-firmware-loader.sh /system/bin/
    adb matrix-sensors-status /system/bin/
   ```
6. Programing FPGA (~1 minute for flashing):

    ```bash
    adb shell matrix-firmware-loader.sh
    ```
you get output like this:

    ```bash
    disable Matrix Creator microcontroller..done
    reconfigurate FPGA and Micro..
    DNA is 0x79ec27f5572e2dfd
    done
    ```

**NOTE** if you shutdown your raspberryPi, please repeat steps: 2 and 6. (root and reprograming FPGA)

Run sensors and everloop demo application
-----------------------------------------

From this point your have a basic Google Things project, for launch Demo (MatrixCreatorGT app) please execute this from main directory:

   ```bash
    ./gradlew installDebug
    adb shell am start admobilize.matrix.gt/.MainActivity
   ```
on your adb logcat will obtain sensors status and everloop leds will are animated.

