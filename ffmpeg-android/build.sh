#!/bin/bash

if [ "$NDK" = "" ]; then
	echo NDK variable not set, assuming ${HOME}/android-ndk
	export NDK=${HOME}/android-ndk
fi

SYSROOT=$NDK/platforms/android-3/arch-arm
# Expand the prebuilt/* path into the correct one
TOOLCHAIN=`echo $NDK/toolchains/arm-linux-androideabi-4.4.3/prebuilt/*-x86`
export PATH=$TOOLCHAIN/bin:$PATH

rm -rf build/ffmpeg
mkdir -p build/ffmpeg
cd ffmpeg

# Don't build any neon version for now
for version in armv5te armv7a; do

# --prefix=PREFIX          install in PREFIX []
#  --bindir=DIR             install binaries in DIR [PREFIX/bin]
#  --datadir=DIR            install data files in DIR [PREFIX/share/ffmpeg]
#  --libdir=DIR             install libs in DIR [PREFIX/lib]
#  --shlibdir=DIR           install shared libs in DIR [PREFIX/lib]
#  --incdir=DIR             install includes in DIR [PREFIX/include]
#  --mandir=DIR             install man page in DIR [PREFIX/share/man]
#  --soname-prefix=PREFIX   add PREFIX before the libraries soname

	DEST=../build/ffmpeg
	FLAGS="--target-os=linux --cross-prefix=arm-linux-androideabi- --arch=arm"
	FLAGS="$FLAGS --datadir=/data/data/org.witness.sscvideoproto/"
	FLAGS="$FLAGS --sysroot=$SYSROOT"
	#FLAGS="$FLAGS --soname-prefix=/data/data/com.bambuser.broadcaster/lib/"
	FLAGS="$FLAGS --soname-prefix=/data/data/org.witness.sscvideoproto/"
	FLAGS="$FLAGS --enable-shared --disable-symver"
	#FLAGS="$FLAGS --enable-static --disable-symver"
	FLAGS="$FLAGS --enable-small --optimization-flags=-O2"
	#FLAGS="$FLAGS --disable-everything"
	#FLAGS="$FLAGS --enable-encoder=libfaac --enable-encoder=libx264"
	FLAGS="$FLAGS --enable-encoder=mpeg2video --enable-encoder=nellymoser"
	FLAGS="$FLAGS --enable-protocol=file"
	FLAGS="$FLAGS --enable-filter=color --enable-filter=vflip --enable-filter=overlay --enable-filter=unsharp"
	FLAGS="$FLAGS --enable-libx264 --enable-gpl"

#./configure --list-filters
#--enable-filter=NAME
#anull			frei0r_src		scale
#anullsink		gradfun			setdar
#anullsrc		hflip			setpts
#blackframe		hqdn3d			setsar
#buffer			noformat		settb
#color			null			slicify
#crop			nullsink		transpose
#cropdetect		nullsrc			unsharp
#drawbox		ocv_smooth		vflip
#fifo			overlay			yadif
#format			pad
#frei0r			pixdesctest


	case "$version" in
		neon)
			EXTRA_CFLAGS="-march=armv7-a -mfloat-abi=softfp -mfpu=neon -I../x264"
			#EXTRA_LDFLAGS="-Wl,--fix-cortex-a8 -L/data/data/org.witness.sscvideoproto"
			EXTRA_LDFLAGS="-Wl,--fix-cortex-a8 -L../x264"
			#EXTRA_LDFLAGS="-Wl,--fix-cortex-a8"
			# Runtime choosing neon vs non-neon requires
			# renamed files
			ABI="armeabi-v7a"
			;;
		armv7a)
			EXTRA_CFLAGS="-march=armv7-a -mfloat-abi=softfp -I../x264"
			#EXTRA_LDFLAGS="-L/data/data/org.witness.sscvideoproto"
			EXTRA_LDFLAGS="-L../x264"
			#EXTRA_LDFLAGS=""
			ABI="armeabi-v7a"
			;;
		*)
			EXTRA_CFLAGS="-I../x264"
			#EXTRA_LDFLAGS="-L/data/data/org.witness.sscvideoproto"
			EXTRA_LDFLAGS="-L../x264"
			#EXTRA_LDFLAGS=""
			ABI="armeabi"
			;;
	esac
	DEST="$DEST/$ABI"
	FLAGS="$FLAGS --prefix=$DEST"

	mkdir -p $DEST
	echo $FLAGS --extra-cflags="$EXTRA_CFLAGS" --extra-ldflags="$EXTRA_LDFLAGS" > $DEST/info.txt
	./configure $FLAGS --extra-cflags="$EXTRA_CFLAGS" --extra-ldflags="$EXTRA_LDFLAGS" | tee $DEST/configuration.txt
	[ $PIPESTATUS == 0 ] || exit 1
	make clean
	make -j4 || exit 1
	make install || exit 1

done

