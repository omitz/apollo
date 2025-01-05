<template>

  <v-card class="ma-4 pa-6"   tile  >
    <div class="canvas-wrapper">
      <v-img
        ref="imageShow"
        class="align-end "
        :height="`${vimgHeight}px`"
        :min-width="`${vimgMinWidth}px`"
        :src="this.formatThumbnail(name)"
        @load="drawMultiple(bbs)"
        @click="dialog = true"
      >
      </v-img>
      <canvas ref="boxOverlay" class="canvas-overlay" :width="`${vimgMinWidth}px`" :height="`${vimgHeight}px`"></canvas>
    </div>
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
            <v-img
                :src="formatLarge(name)"
                contain="contain"
              >
            </v-img>
            <AnalyticResultsView :path="path" />
        </v-dialog>
    </v-card>
  </v-card>
</template>


<script>

import AnalyticResultsView from "./AnalyticResultsView";

export default {
  components: { AnalyticResultsView },

  props:{
      name: String,
      probability: Number,
      path: String,
      bbs: Array,
  },
  name: "ImageView",
  data() {
    return {
      status: "Critical",
      imageName:"",
      mMove: true,
      dialog: false,
      contain: true,
      signedUrl: "", 
      vimgHeight: 125, 
      vimgMinWidth: 150,  
      drawingHeight: 125,
      drawingWidth: 150, 
      aspectRatio: 1,
      window: {
        width: 5, 
        height: 5
      },
      objects_detected: []
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
    handleFocus() {
      this.dialog = false
    },
    drawMultiple(bbs) {
      this.setDrawingDims()
      if (bbs) {
        // console.log(`bbs: ${bbs}`)
        var bb;
        for (bb = 0; bb < bbs.length; bb++) {
          // draw bb_ymin_xmin_ymax_xmax
          var coordinates = bbs[bb]
          // console.log(`coordinates: ${coordinates}`)
          this.draw(coordinates[1], coordinates[3], coordinates[0], coordinates[2]);
        }
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
        // console.log(`Setting drawing width to ${this.drawingWidth}`)
      } else {
        /**
         * If the image is taller than it is wide, the top and bottom will get cut off,
         * so we'll need to base 'y' and 'height' off of the img height when it's resized to have 'vimgMinWidth'
         */ 
        this.drawingHeight = this.vimgMinWidth / this.aspectRatio
        // console.log(`Setting drawing height to ${this.drawingHeight}`)
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
</style>