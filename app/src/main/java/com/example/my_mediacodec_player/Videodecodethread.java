package com.example.my_mediacodec_player;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Videodecodethread extends Thread{
    private static final String TAG = "video-decoder";
    private static  final int time_out =1000;
    private MediaExtractor mediaExtractor;
    private MediaFormat mediaFormat;
    private Surface msurface;
    private MediaCodec mMediacodec;
    private boolean isIOS;
    private long mFrameStamp;

    public Videodecodethread(Surface surface){
        this.msurface = surface;
    }


    public void setDatasource(AssetFileDescriptor fd) {
        try {
            mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            mediaFormat = parserMediaFormat(mediaExtractor);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private MediaFormat parserMediaFormat(MediaExtractor mediaExtractor){
        int trackcount = mediaExtractor.getTrackCount();
        for(int i=0; i<trackcount; i++){
               MediaFormat mediaFormat =mediaExtractor.getTrackFormat(i);
               String MineType = mediaFormat.getString(mediaFormat.KEY_MIME);
               if (MineType.startsWith("video")){
                   mediaExtractor.selectTrack(i);
                   return mediaFormat;
               }
        }
            return null;
    }
    public boolean parpared(){
        if (mediaFormat != null){
            String MineType = mediaFormat.getString(mediaFormat.KEY_MIME);
            try{
                mMediacodec  =MediaCodec.createDecoderByType(MineType);
                mMediacodec.configure(mediaFormat,msurface,null,0);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return  false;
    }

    @Override
    public void run(){
        super.run();
        MediaCodec.BufferInfo outbufferinfo = new MediaCodec.BufferInfo();
        mMediacodec.start();
        ByteBuffer[] inputBuffers =mMediacodec.getInputBuffers();
        while (!isIOS){
                int inputindex = mMediacodec.dequeueInputBuffer(time_out);
                if (inputindex>0){
                    int size = mediaExtractor.readSampleData(inputBuffers[inputindex],0);
                    if (size<0){
                        mMediacodec.queueInputBuffer(inputindex,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }else{
                        mMediacodec.queueInputBuffer(inputindex,0,size,mediaExtractor.getSampleTime(),0);
                        inputBuffers[inputindex].clear();
                        mediaExtractor.advance();
                    }
                    }
                int outputindex = mMediacodec.dequeueOutputBuffer(outbufferinfo,time_out);
                if((outbufferinfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) !=0){
                        isIOS=true;
                        Log.d(TAG, "is EOS BUFFER_FLAG_END_OF_STREAM");
                        break;
                }
                switch (outputindex){
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        MediaFormat outFormat =mMediacodec.getOutputFormat();
                        int width= outFormat.getInteger(MediaFormat.KEY_WIDTH);
                        int height = outFormat.getInteger(MediaFormat.KEY_HEIGHT);
                        Log.d(TAG,"New format" + outFormat + "width=" + width + "height=" + height);
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        try{
                            Thread.sleep(50);
                            break;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    default:
                        mMediacodec.releaseOutputBuffer(outputindex,true);
                        mFrameStamp =outbufferinfo.presentationTimeUs;
                        try {
                            Thread.sleep(40);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;

                    }
                }
        release();
    }
    private void release(){
        if (mediaExtractor != null){
            mediaExtractor.release();
            mediaExtractor = null;
        }
        if (msurface != null){
            msurface.release();
            msurface =null;
        }
        if (mMediacodec !=null){
            mMediacodec.stop();
            mMediacodec.release();
            mMediacodec =null;
        }

    }

}




