// package com.android.cong.mediaeditdemo.audiomux;
package com.caci.apollo.speaker_id_library.MediaEditDemo;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaDataSource;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.io.FileUtils;

// TC 2021-03-18 (Thu) -- modified by Tommy Chang
// https://github.com/xiaokc/MediaEditDemo

/**
 * Created by xiaokecong on 16/06/2017.
 */

public class MuxHelper {
    private static String TAG = "Tommy MuxHelper";

    @SuppressLint("NewApi")
    private class InputStreamSource extends MediaDataSource {
        private byte[] audioBuffer;
        private boolean failed = false;

        public InputStreamSource (InputStream inputStream){
            try {
                audioBuffer = new byte [inputStream.available()];
                inputStream.read (audioBuffer);
                inputStream.close ();
            } catch (IOException e) {
                e.printStackTrace();
                failed = true;  // TBD...
            }
        }
        
        @Override
        public synchronized int readAt(long position, byte[] buffer,
                                       int offset, int size) throws IOException {
            synchronized (audioBuffer){
                int length = audioBuffer.length;
                if (position >= length) {
                    return -1; // -1 indicates EOF
                }
                if (position + size > length) {
                    size -= (position + size) - length;
                }
                System.arraycopy(audioBuffer, (int)position, buffer, offset, size);
                return size;
            }
        }
        
        @Override
        public synchronized long getSize() throws IOException {
            synchronized (audioBuffer) {
                return audioBuffer.length;
            }
        }

        @Override
        public synchronized void close() throws IOException {
            audioBuffer = null;
        }
    }

    
    public static boolean decodeMusicFile (String musicFileUrl,
                                           String decodeFileUrl,
                                           int startSecond, int endSecond,
                                           int uniChannelNum, int uniSampleRate) {
        int sampleRate = 0;
        int channelCount = 0;
        long duration = 0;
        String mime = null;

        MediaExtractor mediaExtractor = new MediaExtractor();
        MediaFormat mediaFormat = null;
        MediaCodec mediaCodec = null;

        try {
            mediaExtractor.setDataSource(musicFileUrl);
        } catch (Exception e) {
            Log.d(TAG, "===>xkc mediaExtractor error");
            return false;
        }

        mediaFormat = mediaExtractor.getTrackFormat(0);
        Log.d (TAG, "mediaFormat = " + mediaFormat);

        sampleRate = mediaFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE) ?
                mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 44100;
        channelCount = mediaFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ?
                mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 1;
        duration = mediaFormat.containsKey(MediaFormat.KEY_DURATION) ? mediaFormat.getLong
                (MediaFormat.KEY_DURATION)
                : 0;
        mime = mediaFormat.containsKey(MediaFormat.KEY_MIME) ? mediaFormat.getString(MediaFormat
                .KEY_MIME) : "";

        Log.d(TAG, "audio Track info: mime:" + mime + " sampleRate:" + sampleRate + " channels:" +
                        channelCount + " duration:" + duration);

        if (TextUtils.isEmpty(mime) || !mime.startsWith("audio/")) {
            Log.d(TAG, "non-audio mime:" + mime);
            return false;
        }

        if (mime.equals("audio/ffmpeg")) {
            mime = "audio/mpeg";
            mediaFormat.setString(MediaFormat.KEY_MIME, mime);
        }

        try {
            mediaCodec = MediaCodec.createDecoderByType(mime);

            mediaCodec.configure(mediaFormat, null, null, 0);
        } catch (Exception e) {
            Log.d(TAG, "===>xkc mediaCodec configure error");
            return false;
        }

        getDecodeData (mediaExtractor, mediaCodec, decodeFileUrl, sampleRate, channelCount,
                       startSecond,  endSecond,  uniChannelNum, uniSampleRate);
        return true;
    }



    private static void getDecodeData (MediaExtractor mediaExtractor, MediaCodec mediaCodec,
                                       String decodeFileUrl, int sampleRate,
                                       int channelCount, int startSecond, int endSecond,
                                       int uniChannelNum, int uniSampleRate) {
        BufferedOutputStream bufferedOutputStream =
            GetBufferedOutputStreamFromFile (decodeFileUrl);
        
        byte[] bytes = getDecodeData (mediaExtractor, mediaCodec,
                                      sampleRate, channelCount, startSecond, endSecond,
                                      uniChannelNum, uniSampleRate);
        try {
            bufferedOutputStream.write(bytes);
            bufferedOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d (TAG, "getDecodeData faild");
            return;
        }
    }


    public byte[] decodeMusicFileStream (InputStream inStream,
                                         int startSecond, int endSecond,
                                         int uniChannelNum, int uniSampleRate) {
        Log.d(TAG, "===> calling decodeMusicFileStream");
        MediaExtractor mediaExtractor = new MediaExtractor();

        // We check for API, if it is >= API 23, then, we use custom MediaDataSource.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            InputStreamSource mediaDataSource =
                new InputStreamSource (inStream); // instream will be closed by InputStreamSource
            try {
                mediaExtractor.setDataSource (mediaDataSource);
            } catch (Exception e) {
                Log.d(TAG, "===> mediaDataSource error");
                return null;
            }
        } else {
            return null;
        }

        return decodeMusic (mediaExtractor, startSecond, endSecond, uniChannelNum, uniSampleRate);
    }


    public byte[] decodeMusicFile (String musicFileUrl, 
                                   int startSecond, int endSecond,
                                   int uniChannelNum, int uniSampleRate) {
        Log.d(TAG, "===> calling decodeMusicFile");
        
        MediaExtractor mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource (musicFileUrl);
        } catch (Exception e) {
            Log.d(TAG, "===>xkc mediaExtractor error");
            return null;
        }

        return decodeMusic (mediaExtractor, startSecond, endSecond, uniChannelNum, uniSampleRate);
    }

    
    private byte[] decodeMusic (MediaExtractor mediaExtractor,
                               int startSecond, int endSecond,
                               int uniChannelNum, int uniSampleRate) {
    
        int sampleRate = 0;
        int channelCount = 0;
        long duration = 0;
        String mime = null;
        MediaFormat mediaFormat = null;
        MediaCodec mediaCodec = null;

        mediaFormat = mediaExtractor.getTrackFormat(0);
        Log.d (TAG, "mediaFormat = " + mediaFormat);

        sampleRate = mediaFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE) ?
                mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 44100;
        channelCount = mediaFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ?
                mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 1;
        duration = mediaFormat.containsKey(MediaFormat.KEY_DURATION) ? mediaFormat.getLong
                (MediaFormat.KEY_DURATION) : 0;
        mime = mediaFormat.containsKey(MediaFormat.KEY_MIME) ? mediaFormat.getString(MediaFormat
                .KEY_MIME) : "";

        Log.d (TAG, " mime:" + mime + " sampleRate:" + sampleRate + " channels:" +
               channelCount + " duration:" + duration);

        // if (!mime.equals("audio/mp4a-latm")) {
        //     Log.d(TAG, "audio is not AAC :" + mime);
        //     return null;
        // }

        try {
            mediaCodec = MediaCodec.createDecoderByType(mime);
            mediaCodec.configure(mediaFormat, null, null, 0);
        } catch (Exception e) {
            Log.d(TAG, "===>mediaCodec creation error");
            return null;
        }

        return getDecodeData (mediaExtractor, mediaCodec, sampleRate, channelCount,
                              startSecond,  endSecond, uniChannelNum, uniSampleRate);
    }

    
    
    private static byte[] getDecodeData (MediaExtractor mediaExtractor, MediaCodec mediaCodec,
                                         int sampleRate,
                                         int channelCount, int startSecond, int endSecond,
                                         int uniChannelNum, int uniSampleRate) {
        boolean decodeInputEnd = false;
        boolean decodeOutputEnd = false;
        ByteArrayOutputStream bufferedOutputStream = new ByteArrayOutputStream();
        
        
        int sampleDataSize;
        int inputBufferIndex;
        int outputBufferIndex;
        int byteNumber;

        long decodeNoticeTime = System.currentTimeMillis();
        long decodeTime;
        long presentationTimeUs = 0;

        final long timeOutUs = 100;
        final long startMicroseconds = startSecond * 1000 * 1000;
        final long endMicroseconds = endSecond * 1000 * 1000;

        ByteBuffer[] inputBuffers;
        ByteBuffer[] outputBuffers;

        ByteBuffer sourceBuffer;
        ByteBuffer targetBuffer;

        MediaFormat outputFormat = mediaCodec.getOutputFormat();

        MediaCodec.BufferInfo bufferInfo;

        byteNumber = (outputFormat.containsKey("bit-width") ?
                      outputFormat.getInteger("bit-width") : 0) / 8;

        mediaCodec.start();

        inputBuffers = mediaCodec.getInputBuffers();
        outputBuffers = mediaCodec.getOutputBuffers();

        mediaExtractor.selectTrack(0);

        bufferInfo = new MediaCodec.BufferInfo();


        Log.d (TAG, "Calling getDecodeData");
        

        while (!decodeOutputEnd) {

            if (!decodeInputEnd) {
            decodeTime = System.currentTimeMillis();
            // try {
                inputBufferIndex = mediaCodec.dequeueInputBuffer(timeOutUs);

                if (inputBufferIndex >= 0) {
                    sourceBuffer = inputBuffers[inputBufferIndex];

                    sampleDataSize = mediaExtractor.readSampleData(sourceBuffer, 0);

                    if (sampleDataSize < 0) {
                        decodeInputEnd = true;
                        sampleDataSize = 0;
                    } else {
                        presentationTimeUs = mediaExtractor.getSampleTime();
                    }

                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, sampleDataSize,
                            presentationTimeUs,
                            decodeInputEnd ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                    if (!decodeInputEnd) {
                        mediaExtractor.advance();
                    }
                } else {
                    Log.d(TAG, "===>xkc inputBufferIndex:"+inputBufferIndex);
                }
            }

                // decode to PCM and push it to the AudioTrack player
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, timeOutUs);

                if (outputBufferIndex < 0) {
                    switch (outputBufferIndex) {
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            outputBuffers = mediaCodec.getOutputBuffers();
                            Log.d(TAG, "===>xkc MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                            break;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            outputFormat = mediaCodec.getOutputFormat();

                            sampleRate = outputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE) ?
                                    outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) :
                                    sampleRate;
                            channelCount = outputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ?
                                    outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) :
                                    channelCount;
                            byteNumber = (outputFormat.containsKey("bit-width") ? outputFormat
                                    .getInteger("bit-width") : 0) / 8;

                            Log.d(TAG, "===>xkc MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                            break;
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            Log.d(TAG, "===>xkc output try again later");
                            break;
                        default:
                            Log.d(TAG, "===>xkc dequeueOutputBuffer returned:"+outputBufferIndex);
                            break;
                    }
                    continue;
                }

                targetBuffer = outputBuffers[outputBufferIndex];

                byte[] sourceByteArray = new byte[bufferInfo.size];

                targetBuffer.get(sourceByteArray);
                targetBuffer.clear();

                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    decodeOutputEnd = true;
                }

                if (sourceByteArray.length > 0 && bufferedOutputStream != null) {
                    if (presentationTimeUs < startMicroseconds) {
                        continue;
                    }

                    byte[] convertByteNumberByteArray = ConvertByteNumber(byteNumber, Constant
                                    .RecordByteNumber,
                            sourceByteArray);

                    byte[] resultByteArray =
                            ConvertChannelNumber(channelCount, uniChannelNum,
                                    Constant.RecordByteNumber,
                                    convertByteNumberByteArray);


                    try {
                        bufferedOutputStream.write(resultByteArray);
                    } catch (Exception e) {
                        Log.d(TAG, "===>xkc bufferedOutputStream write error");
                        return null;
                    }
                }

                if (presentationTimeUs > endMicroseconds) {
                    break;
                }
        }


        ByteArrayOutputStream baos = (ByteArrayOutputStream) bufferedOutputStream;
        byte[] bytes = baos.toByteArray(); // makes a copy
        try {
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Error in closing");
            return null;
        }

        // Log.d (TAG, "closed " + decodeFileUrl);
        if (sampleRate != uniSampleRate) {
            InputStream inputStream = new ByteArrayInputStream(bytes);
            bytes = Resample (sampleRate, inputStream, uniSampleRate);
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Error in closing");
                return null;
            }
        }

        if (bufferedOutputStream != null) {
            try {
                bufferedOutputStream.close();
            } catch (IOException e) {
                Log.d(TAG, "===>xkc bufferedOutputStream close error");
                return null;
            }
        }
        
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
        }

        if (mediaExtractor != null) {
            mediaExtractor.release();
        }

        Log.d (TAG, "done Calling getDecodeData");
        return bytes;
    }


  /**
   * * Convert PCM file to WAV file
   * @param  inPcmFilePath input PCM file path
   * @param  outWavFilePath output WAV file path
   * @param  sampleRate sample rate, for example 44100
   * @param  channels number of channels Mono: 1 or 2 channels: 2
   * @param  bitNum number of samples, 8 or 16
   */
  public static void convertPcm2Wav(String inPcmFilePath, String outWavFilePath, int sampleRate,
      int channels, int bitNum) {

    FileInputStream in = null;
    FileOutputStream out = null;
    byte[] data = new byte[1024];

    try {
      //Sampling byte byte rate
      long byteRate = sampleRate * channels * bitNum / 8;

      in = new FileInputStream(inPcmFilePath);
      out = new FileOutputStream(outWavFilePath);

      //PCM file size
      long totalAudioLen = in.getChannel().size();

      //Total size, because RIFF and WAV are not included, it is 44-8 = 36, plus PCM file size
      long totalDataLen = totalAudioLen + 36;

      writeWaveFileHeader(out, totalAudioLen, totalDataLen, sampleRate, channels, byteRate);

      int length = 0;
      while ((length = in.read(data)) > 0) {
        out.write(data, 0, length);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }


    private static byte[] shortToBytes(short[] shorts) {
        byte[] bytes = new byte[shorts.length * 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);
        return bytes;
    }
    
    public static void convert16BitPcm2Wav(short[] samples,
                                           String outWavFilePath, int sampleRate) {
    FileOutputStream out = null;

    try {
      //Sampling byte byte rate
      long byteRate = sampleRate * 2;

      out = new FileOutputStream(outWavFilePath);

      //PCM file size
      byte[] bytes = shortToBytes(samples);
      int totalAudioLen = bytes.length;

      //Total size, because RIFF and WAV are not included, it is 44-8 = 36, plus PCM file size
      int totalDataLen = totalAudioLen + 36;

      writeWaveFileHeader(out, totalAudioLen, totalDataLen, sampleRate, 1, byteRate);
      Log.d (TAG, "totalDataLen  = " + totalDataLen);
      out.write (bytes, 0, totalAudioLen);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

    
  /**
   * Output WAV file
   * @param  out WAV output file stream
   * @param  totalAudioLen total audio PCM data size
   * @param  totalDataLen total data size
   * @param  sampleRate sample rate
   * @param  channels Number of channels
   * @param  byteRate sample byte byte rate
   * @throws IOException
   */
  private static void writeWaveFileHeader(FileOutputStream out, long totalAudioLen,
      long totalDataLen, int sampleRate, int channels, long byteRate) throws IOException {
    byte[] header = new byte[44];
    header[0] = 'R'; // RIFF
    header[1] = 'I';
    header[2] = 'F';
    header[3] = 'F';
    header[4] = (byte) (totalDataLen & 0xff);//Data size
    header[5] = (byte) ((totalDataLen >> 8) & 0xff);
    header[6] = (byte) ((totalDataLen >> 16) & 0xff);
    header[7] = (byte) ((totalDataLen >> 24) & 0xff);
    header[8] = 'W';//WAVE
    header[9] = 'A';
    header[10] = 'V';
    header[11] = 'E';
    //FMT Chunk
    header[12] = 'f'; // 'fmt '
    header[13] = 'm';
    header[14] = 't';
    header[15] = ' ';//Transition byte
    //Data size
    header[16] = 16; // 4 bytes: size of 'fmt ' chunk
    header[17] = 0;
    header[18] = 0;
    header[19] = 0;
    //Coding method 10H is PCM coding format
    header[20] = 1; // format = 1
    header[21] = 0;
    //Number of channels
    header[22] = (byte) channels;
    header[23] = 0;
    //Sampling rate, playback speed of each channel
    header[24] = (byte) (sampleRate & 0xff);
    header[25] = (byte) ((sampleRate >> 8) & 0xff);
    header[26] = (byte) ((sampleRate >> 16) & 0xff);
    header[27] = (byte) ((sampleRate >> 24) & 0xff);
    //Audio data transfer rate, sampling rate*number of channels*sampling depth/8
    header[28] = (byte) (byteRate & 0xff);
    header[29] = (byte) ((byteRate >> 8) & 0xff);
    header[30] = (byte) ((byteRate >> 16) & 0xff);
    header[31] = (byte) ((byteRate >> 24) & 0xff);
    // Determine how many such bytes of data the system will process at a time, determine the buffer, the number of channels * the number of samples
    header[32] = (byte) (channels * 16 / 8);
    header[33] = 0;
    //Number of data bits per sample
    header[34] = 16;
    header[35] = 0;
    //Data chunk
    header[36] = 'd';//data
    header[37] = 'a';
    header[38] = 't';
    header[39] = 'a';
    header[40] = (byte) (totalAudioLen & 0xff);
    header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
    header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
    header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
    out.write(header, 0, 44);
  }
    
    private static byte[] ConvertByteNumber(int sourceByteNumber, int outputByteNumber, byte[]
            sourceByteArray) {
        if (sourceByteNumber == outputByteNumber) {
            return sourceByteArray;
        }

        int sourceByteArrayLength = sourceByteArray.length;

        byte[] byteArray;

        switch (sourceByteNumber) {
            case 1:
                switch (outputByteNumber) {
                    case 2:
                        byteArray = new byte[sourceByteArrayLength * 2];

                        byte resultByte[];

                        for (int index = 0; index < sourceByteArrayLength; index += 1) {
                            resultByte = CommonFunction.GetBytes((short) (sourceByteArray[index]
                                                                                  * 256), Variable
                                    .isBigEnding);

                            byteArray[2 * index] = resultByte[0];
                            byteArray[2 * index + 1] = resultByte[1];
                        }

                        return byteArray;
                }
                break;
            case 2:
                switch (outputByteNumber) {
                    case 1:
                        int outputByteArrayLength = sourceByteArrayLength / 2;

                        byteArray = new byte[outputByteArrayLength];

                        for (int index = 0; index < outputByteArrayLength; index += 1) {
                            byteArray[index] = (byte) (CommonFunction.GetShort(sourceByteArray[2
                                            * index],
                                    sourceByteArray[2 * index + 1], Variable.isBigEnding) / 256);
                        }

                        return byteArray;
                }
                break;
        }

        return sourceByteArray;
    }

    private static byte[] ConvertChannelNumber(int sourceChannelCount, int outputChannelCount,
                                               int byteNumber,
                                               byte[] sourceByteArray) {
        if (sourceChannelCount == outputChannelCount) {
            return sourceByteArray;
        }

        switch (byteNumber) {
            case 1:
            case 2:
                break;
            default:
                return sourceByteArray;
        }

        int sourceByteArrayLength = sourceByteArray.length;

        byte[] byteArray;

        switch (sourceChannelCount) {
            case 1:
                switch (outputChannelCount) {
                    case 2:
                        byteArray = new byte[sourceByteArrayLength * 2];

                        byte firstByte;
                        byte secondByte;

                        switch (byteNumber) {
                            case 1:
                                for (int index = 0; index < sourceByteArrayLength; index += 1) {
                                    firstByte = sourceByteArray[index];

                                    byteArray[2 * index] = firstByte;
                                    byteArray[2 * index + 1] = firstByte;
                                }
                                break;
                            case 2:
                                for (int index = 0; index < sourceByteArrayLength; index += 2) {
                                    firstByte = sourceByteArray[index];
                                    secondByte = sourceByteArray[index + 1];

                                    byteArray[2 * index] = firstByte;
                                    byteArray[2 * index + 1] = secondByte;
                                    byteArray[2 * index + 2] = firstByte;
                                    byteArray[2 * index + 3] = secondByte;
                                }
                                break;
                        }

                        return byteArray;
                }
                break;
            case 2:
                switch (outputChannelCount) {
                    case 1:
                        int outputByteArrayLength = sourceByteArrayLength / 2;

                        byteArray = new byte[outputByteArrayLength];

                        switch (byteNumber) {
                            case 1:
                                for (int index = 0; index < outputByteArrayLength; index += 2) {
                                    short averageNumber =
                                            (short) ((short) sourceByteArray[2 * index] + (short)
                                                    sourceByteArray[2 *
                                                            index + 1]);
                                    byteArray[index] = (byte) (averageNumber >> 1);
                                }
                                break;
                            case 2:
                                for (int index = 0; index < outputByteArrayLength; index += 2) {
                                    byte resultByte[] = CommonFunction.AverageShortByteArray
                                            (sourceByteArray[2 * index],
                                                    sourceByteArray[2 * index + 1],
                                                    sourceByteArray[2 *
                                                            index + 2],
                                                    sourceByteArray[2 * index + 3], Variable
                                                            .isBigEnding);

                                    byteArray[index] = resultByte[0];
                                    byteArray[index + 1] = resultByte[1];
                                }
                                break;
                        }

                        return byteArray;
                }
                break;
        }

        return sourceByteArray;
    }


    private static byte [] Resample (int         sampleRate,
                                     InputStream fileInputStream,
                                     int         uniSampleRate) {
        Log.d (TAG, "Calling Resample");

        // if (uniSampleRate > sampleRate) {
        //     Log.d(TAG, "not upsampling");
        //     return null;
        // }

        int outSize = 0; 
        byte [] outSamples;
        try {
            outSize = fileInputStream.available();
            Log.d (TAG, "before resample, size= " + outSize);
            outSize = (int) (outSize * ((float) (uniSampleRate + 1024) / sampleRate));
            Log.d (TAG, "after resample, size= " + outSize);
            ByteArrayOutputStream fileOutputStream = new ByteArrayOutputStream (outSize);
            
            new SSRC(fileInputStream, fileOutputStream, sampleRate, uniSampleRate,
                     Constant.RecordByteNumber, Constant.RecordByteNumber, 1, Integer.MAX_VALUE,
                     0, 0, true);
            
            outSamples = fileOutputStream.toByteArray(); // makes a copy
            fileInputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d (TAG, "error resampling");
            return null;
        }
            
        return outSamples;
    }

    
    private static void Resample(int sampleRate, String decodeFileUrl, int uniSampleRate) {
        String newDecodeFileUrl = decodeFileUrl + "new";
        Log.d (TAG, "Calling Resample");
        
        try {
            FileInputStream fileInputStream =
                    new FileInputStream(new File(decodeFileUrl));
            FileOutputStream fileOutputStream =
                    new FileOutputStream(new File(newDecodeFileUrl));

            // SSRC 
            new SSRC(fileInputStream, fileOutputStream, sampleRate, uniSampleRate,
                    Constant.RecordByteNumber, Constant.RecordByteNumber, 1, Integer.MAX_VALUE,
                    0, 0, true);

            fileInputStream.close();
            fileOutputStream.close();

            // FileFunction.RenameFile(newDecodeFileUrl, decodeFileUrl);
            RenameFile(newDecodeFileUrl, decodeFileUrl);
        } catch (IOException e) {
            Log.d(TAG, "===>xkc I/O Stream access error");
            return;
        }
    }


    private static BufferedOutputStream GetBufferedOutputStreamFromFile(String fileUrl) {
        BufferedOutputStream bufferedOutputStream = null;

        try {
            File file = new File(fileUrl);

            if (file.exists()) {
                file.delete();
            }

            file.createNewFile();

            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
        } catch (Exception e) {
            Log.d(TAG, "GetBufferedOutputStreamFromFile error", e);
            return null;
        }

        return bufferedOutputStream;
    }
    
    private static void RenameFile(String oldPath, String newPath) {
        if (CommonFunction.notEmpty(oldPath) && CommonFunction.notEmpty(newPath)) {
            File newFile = new File(newPath);

            if (newFile.exists()) {
                newFile.delete();
            }

            File oldFile = new File(oldPath);

            if (oldFile.exists()) {
                try {
                    oldFile.renameTo(new File(newPath));
                } catch (Exception e) {
                    Log.d(TAG, "can not rename file", e);
                    return;
                }
            }
        }
    }    
}
