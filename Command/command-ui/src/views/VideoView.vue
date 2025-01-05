<template>

  <v-card class="ma-4 pa-6"   tile  >
    <v-img
      class="align-end "
      height="125px"
      min-width ="150px"
      :src="this.formatThumbnail(name)"
    >
    </v-img>
    <v-spacer class="ma-4"></v-spacer>
    <v-card-text class="pa-0">
       <div class="text--primary">  Probability: {{probability}} </div>
       <div class="text-wrap">  Seconds: {{makeSecondsLegible(seconds)}} </div>
       <v-btn
           color="primary"
           type="Submit"
          @click="dialog = true; setSignedUrl(path)">
           Details
       </v-btn>
        <v-dialog
          max-width="615px"
          v-model="dialog">
            <v-card 
              class="player-container"
              color="white"
              max-width="600px">

          <!-- We jump to the first word occurence  -->
          <div>
            <MediaPlayer
              :src="signedUrl"
              :key="signedUrl" 
              style="min-width: 600px"
              mediaType="video"
              :initialTime="seconds[0][0]"
              :seconds="makeSecondsList(seconds)"
            />
          </div>


                <v-card-text class="pa-0">
                    <div class="text--primary">  Seconds: {{makeSecondsLegible(seconds)}} </div>
                </v-card-text>
                <AnalyticResultsView  style="width='900px'" :path="path" />

            </v-card>
        </v-dialog>
    </v-card-text>

  </v-card>
</template>


<script>
// import Vue from 'vue'
// import VueCoreVideoPlayer from 'vue-core-video-player'
// Vue.use(VueCoreVideoPlayer)

import MediaPlayer from "./MediaPlayer";
import AnalyticResultsView from "./AnalyticResultsView";

export default {
  components: { MediaPlayer, AnalyticResultsView },
  props:{
    name: String,
    probability: Number,
    path: String,
    seconds: Array,
  },
  name: "VideoView",
  data() {
    return {
      status: "Critical",
      imageName:"",
      mMove: true,
      signedUrl: "",
      dialog: false
    };
  },
    methods: {
        makeSecondsLegible: function(seconds) {
            var secondsRange;
            var legible = "";
            for (secondsRange = 0; secondsRange < seconds.length; secondsRange++) {
                var start = this.makeSecondLegible(seconds[secondsRange][0]);
                var stop = this.makeSecondLegible(seconds[secondsRange][1]);
                legible += `${start}-${stop}, `
            }
            // slice off the last ", "
            return legible.substring(0, legible.length - 2);
        },
        makeSecondLegible: function(second) {
            var minutes = Math.floor(second / 60)
            var seconds = second - minutes * 60;
            var toReturn = "";
            if (minutes > 0) {
                toReturn += `${minutes}m`;
            }
            toReturn += `${seconds}s`;
            return toReturn;
        },
        makeSecondsList: function(seconds){
          var secondsList = [];
          var idx, elm1, elm2;
          for (idx = 0; idx < seconds.length; idx++) {
            elm1 = seconds[idx][0];
            elm2 = seconds[idx][1]
            secondsList.push(elm1)
            if (elm2 != elm1)
              secondsList.push(seconds[idx][1]);
          }
          // console.log(secondsList)
          return secondsList;
        }
    }

};
</script>

<style scoped>

  .v-card--reveal {
    bottom: 0;
    left:0;
    opacity: 1;
    position: absolute;
    width: 100%;
    height: 100%;
    z-index: 10;
  }

  .dialog {
    margin-left: auto;
    margin-right: auto;
  }

  .player-container {
    width: 600px;
  }
</style>