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

public interface PlaybackListener {
    /**
     * Playback progress updates at 10Hz
     * @param progress time in microsecond since the start of recording
     */
    void onProgress(int progress);
    void onCompletion();
}
