package com.llvision.streamdemo.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @Project: StreamDemo
 * @Description:
 * @Author: haijianming
 * @Time: 2018/11/13 上午11:53
 */
public class AACAudioStream {

    private static final String TAG = "AACAudioStream";
    private static final int SAMPLES_PER_FRAME = 1024;
    private static final int FRAMES_PER_BUFFER = 25;
    private static final int SAMPLING_RATE = 44100;
    private static final int BITRATE = 64000;
    private static final int BUFFER_SIZE = 1920;
    private static final int M_SAMPLING_RATE_INDEX = 0;

    private AudioRecord mAudioRecord;
    private MediaCodec mMediaCodec;
    private ReadThread mReadThread;
    private boolean isReadStart;

    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private ByteBuffer[] mBuffers;
    private MediaFormat mNewFormat;
    private Thread mWriterThread;
    private boolean isWriteStart;

    public AACAudioStream() {
    }

    /**
     * 编码
     */
    public void startRecord() {
        try {
            mMediaCodec = MediaCodec.createEncoderByType("audio/mp4a-latm");
            MediaFormat format = new MediaFormat();
            format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
            format.setInteger(MediaFormat.KEY_BIT_RATE, BITRATE);
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLING_RATE);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, BUFFER_SIZE);
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();

            isWriteStart = true;
            mWriterThread = new WriterThread();
            mWriterThread.start();

            isReadStart = true;
            mReadThread = new ReadThread();
            mReadThread.start();
        } catch (Exception e) {
            Log.e(TAG, "startRecord", e);
        }
    }


    private class ReadThread extends Thread {

        private ReadThread() {
            super("AACRecoder");
        }

        @Override
        public void run() {
            super.run();
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
            int len = 0, bufferIndex = 0;
            try {
                final int min_buffer_size = AudioRecord.getMinBufferSize(
                        SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                int buffer_size = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
                if (buffer_size < min_buffer_size) {
                    buffer_size = ((min_buffer_size / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;
                }
                mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer_size);
                mAudioRecord.startRecording();

                final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
                while (isReadStart) {{
                    bufferIndex = mMediaCodec.dequeueInputBuffer(10000);
                    if (bufferIndex >= 0) {
                        inputBuffers[bufferIndex].clear();
                        len = mAudioRecord.read(inputBuffers[bufferIndex], SAMPLES_PER_FRAME);
                        if (len == AudioRecord.ERROR_INVALID_OPERATION || len == AudioRecord.ERROR_BAD_VALUE) {
                            //                                mMediaCodec.queueInputBuffer(bufferIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            Log.i(TAG, "sent input EOS (with zero-length frame)");
                        } else {
                            if (!isReadStart){
                                Log.i(TAG, "queueInputBuffer end");
                                mMediaCodec.queueInputBuffer(bufferIndex, 0, 0, getPTSUs(), MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            }else {
                                mMediaCodec.queueInputBuffer(bufferIndex, 0, len, getPTSUs(), 0);
                            }

                        }
                    }
                }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (mAudioRecord != null) {
                    mAudioRecord.stop();
                    mAudioRecord.release();
                    mAudioRecord = null;
                }
                if (mMediaCodec != null) {
                    mMediaCodec.stop();
                    mMediaCodec.release();
                    mMediaCodec = null;
                }
            }
        }
    }

    private class WriterThread extends Thread {
        @Override
        public void run() {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                mBuffers = mMediaCodec.getOutputBuffers();
            }

            final ByteBuffer mBuffer = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME).order(ByteOrder.nativeOrder());
            while (isWriteStart) {
                int index = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 10000);
                if (index >= 0) {
                    if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                        continue;
                    }
                    mBuffer.clear();
                    ByteBuffer outputBuffer = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        outputBuffer = mMediaCodec.getOutputBuffer(index);
                    } else {
                        outputBuffer = mBuffers[index];
                    }

                        // write encoded data to muxer(need to adjust presentationTimeUs.
                        mBufferInfo.presentationTimeUs = getPTSUs();
                        prevOutputPTSUs = mBufferInfo.presentationTimeUs;

                    outputBuffer.get(mBuffer.array(), 7, mBufferInfo.size);
                    outputBuffer.clear();
                    mBuffer.position(7 + mBufferInfo.size);
                    addADTStoPacket(mBuffer.array(), mBufferInfo.size + 7);
                    mBuffer.flip();
                    mMediaCodec.releaseOutputBuffer(index, false);
                } else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    mBuffers = mMediaCodec.getOutputBuffers();
                } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    synchronized (AACAudioStream.this) {
                        mNewFormat = mMediaCodec.getOutputFormat();

                    }
                } else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    //                    LogUtil.e(TAG, "No buffer available...");
                } else {

                }
            }
        }
    }

    public void stop() {

        try {
            isWriteStart = false;
            if (mWriterThread != null) {
                mWriterThread.join();
                mWriterThread = null;
            }
        } catch (InterruptedException e) {
            e.fillInStackTrace();
        }

        try {
            isReadStart = false;
            if (mReadThread != null) {
                mReadThread.join();
                mReadThread = null;
            }
        } catch (InterruptedException e) {
            e.fillInStackTrace();
        }
    }

    private static void addADTStoPacket(byte[] packet, int packetLen) {
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF1;
        packet[2] = (byte) (((2 - 1) << 6) + (M_SAMPLING_RATE_INDEX << 2) + (1 >> 2));
        packet[3] = (byte) (((1 & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    /**
     * previous presentationTimeUs for writing
     */
    private long prevOutputPTSUs = 0;
    /**
     * get next encoding presentationTimeUs
     * @return
     */
    protected long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write

        if (result < prevOutputPTSUs)
            result = (prevOutputPTSUs - result) + result;
        return result;
    }

}
