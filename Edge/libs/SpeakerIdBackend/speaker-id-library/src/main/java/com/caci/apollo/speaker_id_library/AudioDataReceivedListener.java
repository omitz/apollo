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

public interface AudioDataReceivedListener {

    /**
     * New data from continuous recording (no time limit)
     * @param data new data since last received
     */
    void onAudioDataReceived(short[] data);

    /**
     * Timed recording progress updates at 10 Hz
     * @param nSamples number of samples received so far
     * @param elapsTimeMs time (in ms) since the start of the recording
     */
    void onAudioDataProgress(int nSamples, long elapsTimeMs);

    /**
     * Returns the completed data (for timed recording only)
     * @param data the complete recording
     */
    void onAudioDataReady(short[] data);
}
