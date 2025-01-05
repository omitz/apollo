<template>

  <v-card class="ma-4 pa-6" tile  @click="dialog = true; setSignedUrl(path); setCanvasDim('https://images.apollo-cttso.com/iiif/2/inputs%2F' + name + '/full/200,/0/default.jpg')">
    <div class="text--primary">File: {{path}}</div>
    <div v-if="parseMediaType() == 'video' || parseMediaType() == 'image'" class="canvas-wrapper">
      <v-img
        ref="imageShow"
        class="align-end "
        :height="`${vimgHeight}px`"
        :min-width="`${vimgMinWidth}px`"
        :src="this.formatThumbnail(name)"
        @load="drawDetectionBoxes(bbs)"
      >
      </v-img>
    <canvas ref="boxOverlay" class="canvas-overlay" :width="`${vimgMinWidth}px`" :height="`${vimgHeight}px`"></canvas>
    </div>
    <v-card-text v-if="parseMediaType() == 'audio' || parseMediaType() == 'text/other'">
      
      <div v-if="snippets">
        <br />
        <p class="text--primary">{{snippets}}</p>
      </div>
      <div v-if="seconds">
        <br />
        <p class="text--primary">{{seconds}}</p>
      </div>
    </v-card-text>
    <v-card 
      @focus="handleFocus"
      tabindex="0"
      >
        <v-dialog
          @focus="handleFocus"
          :max-height="`${window.height*.99}px`"
          :max-width="`${window.width*.5}px`"
          class="dialog"
          v-model="dialog"
        > 
        <div v-if="parseMediaType() === 'image'">
          <div class="canvas-wrapper">
          <v-img
            @load="drawOCRBoxes(polygons)"
            :src="formatLarge(name)"
            contain="contain"
          >
          </v-img>
          <canvas ref="ocrBoxOverlay" 
            class="canvas-overlay" 
            :width="`${ocrDrawingWidth}px`" 
            :height="`${ocrDrawingHeight}px`">
            </canvas>
          </div>
        </div>
          <v-card 
            v-else-if="parseMediaType() === 'video'"
            class="player-container"
            color="white"
          >

          <!-- We jump to the first word occurence  -->
          <div>
            <MediaPlayer
              :src="signedUrl"
              :key="signedUrl" 
              style="width: 100%"
              mediaType="video"
              :initialTime="getInitialTime()"
              :seconds="seconds"
            />
            <div v-if="seconds && seconds.length > 0" class="text-wrap">  Occurences: {{seconds}} </div>
            </div>
          </v-card>
          <v-card 
            v-else-if="parseMediaType() === 'audio'"
            class="player-container"
            color="white"
          >
            <div class="player-container">
              <MediaPlayer
                :src="signedUrl"
                :key="signedUrl" 
                style="width: 100%"
                mediaType="audio"
                :initialTime="getInitialTime()"
                :seconds="seconds"
              />
            <div v-if="seconds && seconds.length > 0" class="text-wrap">  Occurences: {{seconds}} </div>
            </div>
          </v-card>
          <v-card
            v-else
            color="white"
          >
            <v-card-text>
              <div v-if="snippet">
                <div>Snippet:</div>
                <p class="text--primary">
                  {{snippet}}
                </p>
              </div>
              <div v-else-if="snippets">
                <div>Snippets:</div>
                <p class="text--primary">
                  {{snippets}}
                </p>
              </div>
              <div v-if="full_text">
                <div>Full Text:</div>
                <p class="text--primary">
                  {{full_text}}
                </p>
              </div>
              <div class="text--primary">
                File: {{path}}
              </div>
              <v-btn color="primary" type="Submit" @click="toggleNeoGraph(searchterm)">View Graph</v-btn>
              <div v-bind:class="{ neovis: true, open: ner_graph_open, closed: !ner_graph_open }" >
                <div class="graph" :id="'neograph' + index"></div>
              </div>
            </v-card-text>
          </v-card>
          <AnalyticResultsView :path="path" />
        </v-dialog>
    </v-card>
  </v-card>
</template>


<script>
import MediaPlayer from "./MediaPlayer";
import AnalyticResultsView from "./AnalyticResultsView";
import NeoVis from 'neovis.js/dist/neovis.js'; 

export default {
  components: { AnalyticResultsView, MediaPlayer },

  props:{
    name: String,
      probability: Number,
      path: String,
      bbs: Array,
      seconds: Array,
      dialog_open: Boolean,
      polygons: Array,
      snippet: String,
      snippets: String,
      full_text: String,
      searchterm: String,
      index: Number,
  },

  name: "ResultView",
  data() {
    return {
      image_file_types: ['jpg', 'png', 'jpeg', 'gif'],
      video_file_types: ['mp4', 'webm', 'mov', 'm4v'],
      audio_file_types: ['wav', 'mp3', 'm4a'],
      status: "Critical",
      imageName:"",
      mMove: true,
      dialog: this.$props.dialog_open,
      ner_graph_open: false,
      ner_graph_previously_opened: false,
      contain: true,
      signedUrl: "",
      vimgHeight: 125,
      vimgMinWidth: 150,
      vimgMaxDim: 700,
      drawingHeight: 125,
      drawingWidth: 150, 
      ocrDrawingHeight: 125,
      ocrDrawingWidth: 150, 
      aspectRatio: 1,
      window: {
        width: 5, 
        height: 5
      },
      objects_detected: []
    };
  },
  watch: {
    path: function () {
      //watch the "paths" prop and reset dialog open whenever path is changed
      this.dialog = this.$props.dialog_open
      this.setSignedUrl(this.path)
    }
  },
  created() {
    window.addEventListener('resize', this.handleResize);
    this.handleResize();
    this.setSignedUrl(this.path)
  }, 
  destroyed() {
    window.removeEventListener('resize', this.handleResize);
  }, 
  
  methods: {

    getInitialTime() {
      if (this.seconds) {
        return this.seconds[0]
      }
      else return 0;
    },

    parseMediaType() {
      var re = /(?:\.([^.]+))?$/;
      var extension = re.exec(this.$props.path)[1];
      
      if (this.image_file_types.indexOf(extension) > -1) {
        return "image";
      }
      else if (this.video_file_types.indexOf(extension) > -1) {
        return "video";
      }
      else if (this.audio_file_types.indexOf(extension) > -1) {
        return "audio";
      }
      else {
        return "text/other"
      }

    },
    handleResize() {
      this.window.width = window.innerWidth;
      this.window.height = window.innerHeight;
    }, 
    handleFocus() {
      this.dialog = false
    },

    toggleNeoGraph(searchtext) {
      if (this.ner_graph_previously_opened === false) {
        //open for first time, load graph
        this.ner_graph_open = true
        this.ner_graph_previously_opened = true
        this.drawNeoGraph(searchtext)
      }
      else if (this.ner_graph_open === true) {
        this.ner_graph_open = false
      }
      else {
        this.ner_graph_open = true
      }
    },

    drawNeoGraph(searchtext) {
    
      //var neov = require('neovis')
      var login = this.getNeo4jLogin()
      var config = {
        encrypted: login.encryption,
        container_id: "neograph" + this.index,
        server_url: login.url,
        server_user: login.username,
        server_password: login.password,
        labels: {
          "EVENT": {
            "caption":"name"
          }, 
          "FAC": {
            "caption": "name"
          }, 
          "LOC": {
            "caption": "name"
          }, 
          "MONEY": {
            "caption": "name"
          },
          "ORG": {
            "caption": "name"
          },
          "PRODUCT": {
            "caption": "name"
          },
          "MISC": {
            "caption": "name"
          },
          "PERSON": {
            "caption": "name",
          },
          "NORP": {
            "caption": "name",
          },
          "DOCUMENT": {
            "caption": "name"
          }
        },
        relationships: {
          "MENTIONED_IN": {
            "thickness": false,
            //"caption": "MENTIONED_IN"
          },
          "THREAT_IN": {
            "thickness": false,
            //"caption": "THREAT_IN"
          }
        },
      };

      if (searchtext === null) {
        config.initial_cypher = "MATCH (n) WHERE id(n) = 1 RETURN *"
      }
      else {
        config.initial_cypher = "MATCH (n)-[r]->(m:DOCUMENT)<-[r2]-(n2) where n.name=\"" + searchtext + "\" return *"
      }
      var viz = new NeoVis(config);
      viz.render();
    },

    drawDetectionBoxes(bbs) {
      //draw method for face and obj-det boxes
      this.setDrawingDims()
      if (bbs) {
        // console.log(`bbs: ${bbs}`)
        var bb;
        for (bb = 0; bb < bbs.length; bb++) {
          // draw bb_ymin_xmin_ymax_xmax
          var coordinates = bbs[bb]
          this.draw(coordinates[1], coordinates[3], coordinates[0], coordinates[2]);
        }
      }
    }, 

    drawOCRBoxes(bbs) {
      //draw method for ocr boxes
      console.log("drawing height: " + this.ocrDrawingHeight);
      console.log("drawing width: " + this.ocrDrawingWidth);
      if (bbs) {

        const canvas = this.$refs['ocrBoxOverlay'].getContext('2d')

        var idx;
        for (idx = 0; idx < bbs.length; idx++) {
          var x;
          var y;

          canvas.beginPath()
            canvas.strokeStyle="#00FF00"
          // canvas.rect(x, y, width, height)   // x,y,width,height

            x = this.ocrDrawingWidth * bbs[idx][0][0];
            y = this.ocrDrawingHeight * bbs[idx][0][1];
            canvas.moveTo(x, y);

            var c_idx;
            for (c_idx = 1; c_idx < bbs[idx].length; c_idx++){
              x = this.ocrDrawingWidth * bbs[idx][c_idx][0];
              y = this.ocrDrawingHeight * bbs[idx][c_idx][1];
              canvas.lineTo(x, y);
            }
            canvas.closePath();

          canvas.stroke()

          // canvas.fillRect(x, y, 60,115)
          // canvas.clearRect(0, 0, 50, 50);
        }
      }

    },

    setCanvasDim (img_src){
      // This only works if img_src has already been loaded.
      const img = new Image();
      img.src = img_src;

      const naturalHeight = img.height;
      const naturalWidth = img.width;

      console.log (`naturalHeight = ${naturalHeight}`)
      console.log (`naturalWidth = ${naturalWidth}`)

      var w2hRatio = naturalWidth / naturalHeight

      /**
       * Width and Height must always be < vimgMaxDim. 
       */
      if (w2hRatio > 1) {
        this.ocrDrawingWidth = this.vimgMaxDim;
        this.ocrDrawingHeight = this.vimgMaxDim / w2hRatio;
      } else {
        this.ocrDrawingHeight = this.vimgMaxDim;
        this.ocrDrawingWidth = this.vimgMaxDim * w2hRatio;
      }
    },

    setDrawingDims() {
      /**
       * When displaying the thumbnail, Vue will zoom in on the image such that it will fill the card both height-wise and width-wise, 
       * effectively "zooming in". So, when we draw the bounding boxes, we need to adjust our starting x or starting y accordingly. 
       * To help with this, will update either drawingWidth or drawingHeight based on the aspect ratio of the image.
       */
      const {naturalHeight, naturalWidth} = this.$refs.imageShow.image
      this.aspectRatio = naturalWidth / naturalHeight
      // console.log(`aspect ratio: ${this.aspectRatio}`)
      /**
       * If the image is wider than it is tall, the sides of the image will get cut off, 
       * so we'll need to base 'x' and 'width' off of the img width when it's resized to have 'vimgHeight'
       */
      if (this.aspectRatio > 1) {
        this.drawingWidth = this.vimgHeight * this.aspectRatio
        console.log(`Setting drawing width to ${this.drawingWidth}`)
      } else {
        /**
         * If the image is taller than it is wide, the top and bottom will get cut off,
         * so we'll need to base 'y' and 'height' off of the img height when it's resized to have 'vimgMinWidth'
         */ 
        this.drawingHeight = this.vimgMinWidth / this.aspectRatio
        console.log(`Setting drawing height to ${this.drawingHeight}`)
      }
    }, 
    draw(xmin, xmax, ymin, ymax) {
      var x = this.drawingWidth * xmin
      var y = this.drawingHeight * ymin
      // Account for the portion of the image that gets cut off
      if (this.aspectRatio > 1) {
        x = x - ((this.drawingWidth - this.vimgMinWidth)/2)
      } else {
        y = y - ((this.drawingHeight - this.vimgHeight)/2)
      }
      const width = this.drawingWidth * (xmax - xmin)
      const height = this.drawingHeight * (ymax - ymin)
      
      const canvas = this.$refs['boxOverlay'].getContext('2d')
      canvas.beginPath()
      canvas.strokeStyle="#00FF00"
      canvas.rect(x, y, width, height)
      // console.log(`x, y, width, height: ${x}, ${y}, ${width}, ${height}}`)
      canvas.stroke()
    }
  },

  beforeMounted() {
    
  }

};
</script>

<style scoped>

  .canvas-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 162;
    bottom: 125;
    pointer-events: none;
    width: 100%;
    height: 100%
  }

  .canvas-wrapper {
    position: relative
  }

  .dialog {
    margin-left: auto;
    margin-right: auto;
  }

  .player-container {
    width: 100%;
  }

  .v-card--reveal {
    bottom: 0;
    left:0;
    opacity: 1;
    position: absolute;
    width: 100%;
    height: 100%;
    z-index: 10;
  }

</style>