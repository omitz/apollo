/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

// package com.newventuresoftware.waveformdemo;
package com.caci.apollo.speaker_id_library;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import static java.lang.Integer.min;

public class RecordingThread {
    // private static final String LOG_TAG = RecordingThread.class.getSimpleName();
    private static final String LOG_TAG = "Tommy Recording Thread";
//    private static final int SAMPLE_RATE = 44100;
    private static final int SAMPLE_RATE = 16000;
    private static int m_maxRecordingLen = 0;
    short [] m_audioData = null;

    public RecordingThread(AudioDataReceivedListener listener) {
        mListener = listener;
        // m_maxRecordingLen = (int) (recordingLen_sec * SAMPLE_RATE);
        // m_audioData = new short [m_maxRecordingLen];
        // Log.d (LOG_TAG, "allocated " + m_maxRecordingLen);
        
    }

    private boolean mShouldContinue;
    private AudioDataReceivedListener mListener;
    private Thread mThread;

    public boolean recording() {
        return mThread != null;
    }

    public void startRecording() {
        m_maxRecordingLen = 0;
        
        if (mThread != null)
            return;

        mShouldContinue = true;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                record();
            }
        });
        mThread.start();
    }

    /**
     * Start a recording of specific time duration
     * @param recordingLen_sec recording length in seconds
     */
    public void startRecording(int recordingLen_sec) {
        m_maxRecordingLen = (int) (recordingLen_sec * SAMPLE_RATE);
        m_audioData = new short [m_maxRecordingLen];
        Log.d (LOG_TAG, "allocated " + m_maxRecordingLen);
        
        if (mThread != null)
            return;
        
        mShouldContinue = true;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                record();
            }
        });
        mThread.start();
    }



    public void stopRecording() {
        if (mThread == null)
            return;

        mShouldContinue = false;
        mThread = null;
    }

    private void record() {
        Log.v(LOG_TAG, "Start");
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

        // buffer size in bytes
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }

        short[] audioBuffer = new short[bufferSize / 2];

        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            return;
        }
        record.startRecording();

        Log.v(LOG_TAG, "Start recording");

        if (m_maxRecordingLen == 0) { // continuous recording
            long shortsRead = 0;
            while (mShouldContinue) {
                int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
                shortsRead += numberOfShort;
                mListener.onAudioDataReceived (audioBuffer);
            }
            record.stop();
            record.release();
            Log.v(LOG_TAG, String.format("Recording stopped. Samples read: %d", shortsRead));
            return;
        }

        // Timed recording
        int shortsRead = 0;
        long notifyRead = 0;
        final long maxNotifyRead = 1600; // 10 hz = (16k samples per second) / 10
        int optBufferSize = bufferSize / 2;
        int audioDataLen = m_audioData.length;
        spkrIdUtils.Assert.that (optBufferSize < audioDataLen, "");
        long startTime = System.currentTimeMillis();
        while (shortsRead < audioDataLen) {
            int shortToRead = min ((audioDataLen - shortsRead), optBufferSize);
            int numberOfShort = record.read (m_audioData, shortsRead, shortToRead);
            shortsRead += numberOfShort;
            notifyRead += numberOfShort;

            // don't notify too frequently
            if (notifyRead > maxNotifyRead)
                {
                    notifyRead = 0;
                    mListener.onAudioDataProgress (shortsRead,
                                                   System.currentTimeMillis() - startTime);
                }
        }
        spkrIdUtils.Assert.that (shortsRead == audioDataLen, "");
        
        record.stop();
        record.release();
        stopRecording();        // TC 2021-01-13 (Wed) --
        mListener.onAudioDataReady(m_audioData);

        Log.v(LOG_TAG, String.format("Recording stopped. Samples read: %d", shortsRead));
    }
}
