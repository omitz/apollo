<template>

  <v-card class="ma-4 pa-6" tile >
    <v-img v-if="mime_type === 'video'"
      class="align-end "
      height="125px"
      min-width ="150px"
      :src="this.formatThumbnail(convertImageName(path))"
    >
    </v-img>
    <v-spacer class="ma-4"></v-spacer>
    <v-card-text class="pa-0" >
        <div class="text--primary">  File: {{path}} </div>
        <div class="text-wrap">  Recognition prediction: {{prediction}} </div>
        <v-btn
            color="primary"
            type="Submit"
          @click="dialog=true; setSignedUrl(path)">
            Details
        </v-btn>
        <v-dialog
         v-model="dialog"
         max-width="615px"
         >
            <v-card
              color="white"
              max-height="900px"
              max-width="900px"
              class="player-container">
                <div>
                  <MediaPlayer
                    :src="signedUrl"
                    :key="signedUrl" 
                    :mediaType="mime_type ? mime_type : 'audio'"
                    style="min-width: 600px"
                    :initialTime="0"
                  />
                </div>
                <AnalyticResultsView :path="path" />
            </v-card>
        </v-dialog>
    </v-card-text>
  </v-card>
</template>


<script>

import MediaPlayer from "./MediaPlayer";
import AnalyticResultsView from "./AnalyticResultsView"

export default {
  components: { MediaPlayer, AnalyticResultsView },
  props:{
      name: String,
      path: String,
      prediction: String, 
      mime_type: String,
  },
  name: "SpeakerView",
  data() {
    return {
      signedUrl: "",
      dialog: false
    };
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