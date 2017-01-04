#!/usr/bin/env sh
set -e

if [ -z "$NDK_ROOT" ] && [ "$#" -eq 0 ]; then
    echo 'Either $NDK_ROOT should be set or provided as argument'
    echo "e.g., 'export NDK_ROOT=/path/to/ndk' or"
    echo "      '${0} /path/to/ndk'"
    exit 1
else
    NDK_ROOT="${1:-${NDK_ROOT}}"
fi

ANDROID_ABI=${ANDROID_ABI:-"armeabi-v7a with NEON"}
WD=$(readlink -f "`dirname $0`/..")
N_JOBS=${N_JOBS:-4}
XC3PROG_ROOT=${WD}/xc3sprog
BUILD_DIR=${XC3PROG_ROOT}/build
ANDROID_LIB_ROOT=${WD}/android_lib
APP_PLATFORM="android-9"

ASFLAGS="-D__ANDROID__"

#rm -rf "${BUILD_DIR}"
mkdir -p "${BUILD_DIR}"
cd "${BUILD_DIR}"

cmake -DCMAKE_TOOLCHAIN_FILE="${WD}/android-cmake/android.toolchain.cmake" \
      -DANDROID_NDK="${NDK_ROOT}" \
      -DCMAKE_BUILD_TYPE=Debug \
      -DANDROID_ABI="${ANDROID_ABI}" \
      -DANDROID_NATIVE_API_LEVEL=21 \
      -DADDITIONAL_FIND_PATH="${ANDROID_LIB_ROOT}" \
      -DUSE_LMDB=ON \
      -DUSE_LEVELDB=OFF \
      -DUSE_HDF5=OFF \
      -DCMAKE_INSTALL_PREFIX="${ANDROID_LIB_ROOT}/xc3sprog" \
      ..

make -j${N_JOBS}
rm -rf "${ANDROID_LIB_ROOT}/xc3sprog"
make install/strip

if [ -f "${ANDROID_LIB_ROOT}/xc3sprog/lib/libxc3loader.so" ]; then
    echo "-- Installing on Android app libs: libxc3loader.so ==> app/src/main/libs/armeabi-v7a"
    mkdir -p "${WD}/app/src/main/libs/armeabi-v7a/"
    cp "${ANDROID_LIB_ROOT}/xc3sprog/lib/libxc3loader.so" "${WD}/app/src/main/libs/armeabi-v7a/"
    echo -n "-- Details: "
    echo `ls -Ggs --time-style=iso "${WD}/app/src/main/libs/armeabi-v7a/libxc3loader.so" | sed "s|${WD}/app/src/main/libs/armeabi-v7a/||g"`
fi
cd "${WD}"
