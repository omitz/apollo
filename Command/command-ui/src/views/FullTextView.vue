<template>
<!--
This component is used to display the results of speech to text, OCR, and sentiment analysis. 
-->

  <v-card class="ma-4 pa-6" tile>
    <v-card-text>
      <div>{{service_name}}: Found text in</div>
      <p class="text--primary">{{snippet}}</p>
      <div class="text--primary">File: {{path}}</div>


    </v-card-text>
    <v-card-text class="pa-0">
      <div v-if="service_name==='speech_to_text'">
        <v-card-text>
          <div class="text--primary">Seconds: {{seconds}}</div>
        </v-card-text>
        <v-btn
          color="primary"
          type="Submit"
          @click="dialog = true; setSignedUrl(path)"
        >Details</v-btn>
      </div>
      <div v-else-if="service_name==='text_sentiment'">
        <v-card-text>
          <div class="text--primary">Sentiment: {{sentiment}}</div>
          <div class="text--primary">Polarity: {{polarity}}</div>
        </v-card-text>
      </div>
      <div v-else>
          <v-img
            class="align-end"
            max-width ="150px"
            :src="$prefixObjDetectLocation + name +  $suffixObjDetectLocation"
            @click="dialog = true; setCanvasDim($prefixObjDetectLocation + name +  $suffixObjDetectLocation)"
          >
          </v-img>   
      </div>
      <v-dialog 
        v-model="dialog"
        class="dialog" 
        :max-width="`600px`"
        v-if="service_name==='speech_to_text'"
      >
        <v-card color="white">

          <!-- <div class="player-container"> -->
            <!--We wait until the user clicks "Play video" to make the API call for the signed url. :key="signedUrl" tells this element to rerender whenever the value of signedUrl changes-->
            <!--There's a known bug with this video player that in some cases the video will play but the progress bar will not show the time. autoplay=false is a workaround for that bug. https://github.com/core-player/vue-core-video-player/issues/24-->
            <!--If the vue-core-video-player can't play the file, we'll try to open the file in a new tab. In some cases, this downloads the file so the user can play it with whatever local media player they prefer.-->
<!--             <vue-core-video-player
              v-if="signedUrl.length>0"
              :key="signedUrl"
              :src="signedUrl"
              :autoplay="false"
              @error="openInNewTab(path)"
              style="min-width: 400px"
            ></vue-core-video-player>            
 -->          <!-- </div> -->
          
          <!-- We jump to the first word occurence  -->
          <div class="player-container">
            <MediaPlayer
              :src="signedUrl"
              :key="signedUrl" 
              style="min-width: 400px"
              mediaType="audio"
              :initialTime="seconds[0]"
              :seconds="seconds"
            />
          </div>

          <v-card-text class="pa-0">
            <div class="text--primary">Seconds: {{seconds}}</div>
          </v-card-text>
          <AnalyticResultsView
            :path="path"
            />
        </v-card>
      </v-dialog>


      <v-card-text class="pa-0"  
      v-if="service_name==='ocr_keras' || 
        service_name==='ocr_tesseract' || service_name==='ocr_easy'">
        <v-dialog
          :max-height="`${this.window.height*.99}px`"
          :max-width="`${this.window.width*.7}px`"
          class="dialog"
          v-model="dialog"
        >
            
          <div class="canvas-wrapper">
            <v-img
                ref="imageShow"
                :src="this.formatLarge(name)"
                @load="drawMultiple(polygons)"
                contain="contain"
                max-height="1100px"
                max-width="1300px">
            </v-img>
            <canvas ref="boxOverlay" 
            class="canvas-overlay" 
            :width="`${drawingWidth}px`" 
            :height="`${drawingHeight}px`">
            </canvas>
          </div>
          <AnalyticResultsView
            :path="path"
          />
        </v-dialog>
    </v-card-text>
    
    <v-card-text class="pa-0"  
      v-if="service_name==='text_sentiment'">
        <v-dialog
          :max-height="`${this.window.height*.99}px`"
          :max-width="`${this.window.width*.7}px`"
          class="dialog"
          v-model="dialog"
        >
          <div class="canvas-wrapper">
          </div>
          <AnalyticResultsView
            :path="path"
          />
        </v-dialog>
    </v-card-text>

    </v-card-text>
  </v-card>
</template>


<script>

import MediaPlayer from "./MediaPlayer";
import AnalyticResultsView from "./AnalyticResultsView";

export default {
  components: { MediaPlayer, AnalyticResultsView },
  props: {
    name: String,
    probability: Number,
    path: String,
    seconds: Array,
    polygons: Array,
    snippet: String,
    service_name: String,
    sentiment: String,
    polarity: Number
  },
  name: "FullTextView",
  data() {
    return {
      imageName: "",
      mMove: true,
      signedUrl: "",
      dialog: false,
      vimgMaxDim: 300,
      drawingHeight: 0,
      drawingWidth: 0,
      window: {
        width: 5, 
        height: 5
      },
      // polygons:[[[0.1,0.1],[0.2,0.1],[0.2,0.2],[0.1,0.2]]],
      };
  },

  created() {
    window.addEventListener('resize', this.handleResize);
    this.handleResize();
  }, 
  
  destroyed() {
    window.removeEventListener('resize', this.handleResize);
  },

  methods: {

    handleResize() {
      this.window.width = window.innerWidth;
      this.window.height = window.innerHeight;
    }, 

    setCanvasDim (img_src){
      // This only works if img_src has already been loaded.
      const img = new Image();
      img.src = img_src;

      const naturalHeight = img.height;
      const naturalWidth = img.width;

      // console.log (`naturalHeight = ${naturalHeight}`)
      // console.log (`naturalWidth = ${naturalWidth}`)

      var w2hRatio = naturalWidth / naturalHeight

      /**
       * Width and Height must always be < vimgMaxDim. 
       */
      if (w2hRatio > 1) {
        this.drawingWidth = this.vimgMaxDim;
        this.drawingHeight = this.vimgMaxDim / w2hRatio;
      } else {
        this.drawingHeight = this.vimgMaxDim;
        this.drawingWidth = this.vimgMaxDim * w2hRatio;
      }
    },

    drawMultiple(bbs) {
    
      if (bbs) {
        const canvas = this.$refs['boxOverlay'].getContext('2d')

        var idx;
        for (idx = 0; idx < bbs.length; idx++) {

          var x;
          var y;

          canvas.beginPath()
            canvas.strokeStyle="#00FF00"
          // canvas.rect(x, y, width, height)   // x,y,width,height

            x = this.drawingWidth * bbs[idx][0][0];
            y = this.drawingHeight * bbs[idx][0][1];
            canvas.moveTo(x, y);

            var c_idx;
            for (c_idx = 1; c_idx < bbs[idx].length; c_idx++){
              x = this.drawingWidth * bbs[idx][c_idx][0];
              y = this.drawingHeight * bbs[idx][c_idx][1];
              canvas.lineTo(x, y);
            }
            canvas.closePath();

          canvas.stroke()

          // canvas.fillRect(x, y, 60,115)
          // canvas.clearRect(0, 0, 50, 50);
        }
      }

    }, 
  } 
};
</script>

<style scoped>
.v-card--reveal {
  bottom: 0;
  left: 0;
  opacity: 1;
  position: absolute;
  width: 100%;
  height: 100%;
  z-index: 10;
}
  .canvas-overlay {
    position: absolute;
    top: 0;
    left: 0;
    pointer-events: none;
    width: 100%;
    height: 100%;
    z-index: 200;
  }

  .canvas-wrapper {
    position: relative
  }
  
  .dialog {
    margin-left: auto;
    margin-right: auto;
  }
  .player-container {
    width: 600px;
  }
</style>
