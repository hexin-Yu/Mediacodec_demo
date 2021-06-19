package com.example.my_mediacodec_player;

import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Audiodecodethread extends Thread{
    private MediaExtractor mediaExtractor;
    private MediaFormat mediaFormat;
    private AudioTrack mAudioTrack;

    private int mSampleRateInHz;
    private int mChannels;
    private long timestamp;
    private  boolean isEos;
    private MediaCodec mediaCodec;
    private  static  final int time_out =1000;
    private static final String TAG = "video-decoder";

    public void  setDatasource(AssetFileDescriptor as)  {
        try{
        mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(as.getFileDescriptor(), as.getStartOffset(), as.getLength());
        mediaFormat=paraseMediaFormat(mediaExtractor);
    }catch (IOException e){
            e.printStackTrace();
        }
    }

    private MediaFormat paraseMediaFormat(MediaExtractor mediaExtractor) {
        int trackcount = mediaExtractor.getTrackCount();
        for (int i =0;i<trackcount; i++){
            MediaFormat mediaFormat = mediaExtractor.getTrackFormat(i);
            String MINE_TYPE = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (MINE_TYPE.startsWith("audio")){
                mediaExtractor.selectTrack(i);
                mSampleRateInHz =mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                mChannels = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                return mediaFormat;
            }
        }
        return null;
    }
    public boolean parpared(){
        if (mediaFormat != null) {
            String MINE_TYPE = mediaFormat.getString(MediaFormat.KEY_MIME);

            try {
                mediaCodec = MediaCodec.createDecoderByType(MINE_TYPE);
                mediaCodec.configure(mediaFormat, null, null, 0);
                return  true;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return false;
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void run(){
        super.run();
        int buffersize = AudioTrack.getMinBufferSize(mSampleRateInHz, AudioFormat.CHANNEL_IN_STEREO,AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRateInHz, AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, buffersize, AudioTrack.MODE_STREAM);
        mAudioTrack.play();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        mediaCodec.start();
        ByteBuffer[] inputbuffer = mediaCodec.getInputBuffers();
        while (! isEos){
            int inputIndex = mediaCodec.dequeueInputBuffer(time_out);
            if (inputIndex>=0) {
                int size = mediaExtractor.readSampleData(inputbuffer[inputIndex], 0);
                if (size<0){
                    mediaCodec.queueInputBuffer(inputIndex,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }else{
                    mediaCodec.queueInputBuffer(inputIndex,0,size,mediaExtractor.getSampleTime(),0);
                    inputbuffer[inputIndex].clear();
                    mediaExtractor.advance();
                }
            }
            int outputindex = mediaCodec.dequeueOutputBuffer(bufferInfo,time_out);
            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                isEos = true;
                Log.d(TAG, "is EOS BUFFER_FLAG_END_OF_STREAM");
                break;
            }
            switch (outputindex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    MediaFormat outFormat = mediaCodec.getOutputFormat();
                    mSampleRateInHz = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    mAudioTrack.setPlaybackRate(mSampleRateInHz);
                    Log.d(TAG, "New format " + outFormat + "mSampleRateInHz=" + mSampleRateInHz);
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    Log.d(TAG, "dequeueOutputBuffer timed out!");
                    break;
                default:
                    timestamp =bufferInfo.presentationTimeUs;
                    ByteBuffer outputbuffer = mediaCodec.getOutputBuffer(outputindex);
                    final byte[] chunk = new byte[bufferInfo.size];
                    outputbuffer.get(chunk);
                    mAudioTrack.write(chunk,bufferInfo.offset,bufferInfo.offset+bufferInfo.size);
                    mediaCodec.releaseOutputBuffer(outputindex,true);
                    break;

        }}
        release();
    }

    private void release() {
        if (mAudioTrack != null) {
            mAudioTrack.release();
            mAudioTrack = null;
        }
        if (mediaExtractor != null) {
            mediaExtractor.release();
            mediaExtractor = null;
        }

        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
    }

}
