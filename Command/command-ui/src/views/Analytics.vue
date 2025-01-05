<template>
  <v-main>
    <v-spacer></v-spacer>
    <v-container class="grey lighten-5" id="test" fluid>
      <v-row>
        <v-col cols="6" md="4">
          <v-card class="pa-2" tile height="1350px">
            <!--@submit="myFunc" @submit.prevent allows the user to keyboard press Enter instead of mouse clicking Submit (without reloading the page)-->

            <v-form  @submit="searchDatabase" @submit.prevent>
              <v-spacer class="ma-6"></v-spacer>
              <v-text-field 
                v-model="searchtextobject" 
                label="Enter object tag to search i.e. boat, dog" 
                @click="setDefaults" 
                v-on:keyup.tab="setDefaults">
              </v-text-field>
            </v-form>
            <v-btn color="primary" type="Submit" @click="searchDatabase">Search</v-btn>

            <v-form @submit="searchVidDatabase" @submit.prevent >
              <v-spacer class="ma-6"></v-spacer>
              <v-text-field 
                v-model="searchtextvid" 
                label="Enter object (in video) tag to search i.e. person, book" 
                @click="setDefaults" 
                v-on:keyup.tab="setDefaults">
              </v-text-field>
            </v-form>
            <v-btn color="primary" type="Submit" @click="searchVidDatabase">Search</v-btn>

            <v-form>
              <v-card-text></v-card-text>
              <v-file-input
                id="uploadFace"
                accept="image/*"
                show-size
                label="Upload face to search"
                v-model="file"
                @click="setDefaults();doClear('uploadFace')"
                v-on:keyup.tab="setDefaults">
              </v-file-input>
            </v-form>
            <v-btn color="primary" type="Submit" @click="searchImage">Search</v-btn>

            <v-form>
              <v-card-text></v-card-text>
              <v-file-input
                id="uploadLandmark"
                accept="image/*"
                show-size
                label="Upload landmark to search"
                v-model="fileLandmark"
                @click="setDefaults();doClear('uploadLandmark')"
                v-on:keyup.tab="setDefaults">
              </v-file-input>
            </v-form>
            <v-btn color="primary" type="Submit" @click="searchLandmark">Search</v-btn>

            <br />
            <br />
            <v-form @submit="searchScene" @submit.prevent>
              <v-text-field
                v-model="searchscene"
                label="Enter scene tag to search i.e. indoor, outdoor, food court"
                @click="setDefaults" 
                v-on:keyup.tab="setDefaults">
              </v-text-field>
            </v-form>
            <v-btn color="primary" type="Submit" @click="searchScene">Search</v-btn>

            <br />
            <br />
            <v-form @submit="searchNamedEntity" @submit.prevent>
              <v-text-field 
                v-model="searchentity" 
                label="Enter for an entity" 
                @click="setDefaults" 
                v-on:keyup.tab="setDefaults">
              </v-text-field>
            </v-form>
            <v-btn color="primary" type="Submit" @click="searchNamedEntity">Search</v-btn>

            <br />
            <br />
            <v-form @submit="searchAudioImage" @submit.prevent>
              <v-text-field 
                v-model="searchaudio_image" 
                label="Search audio file or Image" 
                @click="setDefaults" 
                v-on:keyup.tab="setDefaults">
              </v-text-field>
            </v-form>
            <v-btn color="primary" type="Submit" @click="searchAudioImage">Search</v-btn>

            <v-form>
              <v-card-text></v-card-text>
              <v-file-input
                id="uploadSpeaker"
                accept="audio/*,video/*"
                show-size
                label="Upload voice recording to search"
                v-model="fileSpeaker"
                @click="setDefaults();doClear('uploadSpeaker')" 
                v-on:keyup.tab="setDefaults">
              </v-file-input>
            </v-form>
            <v-btn color="primary" type="Submit" @click="searchSpeaker">Search</v-btn>
          
            <v-form  @submit="searchByFilename" @submit.prevent>
              <v-spacer class="ma-6"></v-spacer>
              <v-select 
                v-model="searchfilename"
                :options="uploaded_files"
                :reduce="file => file.name"
              >
              </v-select>
            </v-form>
            <div class="space"></div>
            <v-btn color="primary" type="Submit" @click="searchByFilename">Search</v-btn>

            <br />
            <br />
            <v-form @submit="searchSentiment" @submit.prevent>
              <v-text-field 
                v-model="searchsentiment" 
                label="Enter 'positive', 'negative', or 'neutral'" 
                @click="setDefaults" 
                v-on:keyup.tab="setDefaults">
              </v-text-field>
            </v-form>
            <v-btn color="primary" type="Submit" @click="searchSentiment">Search</v-btn>

          </v-card>
        </v-col>
        <v-spacer></v-spacer>

        <v-col cols="12" md="8">
          <v-card class="pa-2" tile height="auto" min-height="1300px">
            <v-card-title>Results: {{msg}}</v-card-title>
            <!-- Display Search Result --->
            <v-col cols="12">
              <v-row class="white lighten-5" style="height: auto; width:auto; ">
                <!--For each searchType, ':key' should a unique identifier for each element in apollo_data-->
                <template v-if="searchType==='video'">
                  <video-view
                    v-for="mydata in apollo_data"
                      :key="mydata.path"
                      :name="convertImageName(mydata.path)"
                      :probability="mydata.detection_score"
                      :path="mydata.path"
                      :seconds="mydata.seconds"
                      style="max-width: 210px"
                  ></video-view>
                </template>
                <!--Note: For 'landmark', probability is irrelevant-->
                <template v-else-if="searchType==='scene'">
                  <apollo-image-view
                    v-for="mydata in apollo_data"
                    :key="mydata.id"
                    :name="convertImageName(mydata.path)"
                    :probability="Math.round((mydata.detection_score *100))"
                    :path="mydata.path"
                    :bbs="mydata.bb_ymin_xmin_ymax_xmax"
                    style="max-width: 210px"
                  ></apollo-image-view>
                </template>
                <template v-else-if="searchType==='landmark'">
                  <apollo-image-view
                    v-for="mydata in apollo_data"
                    :key="mydata.path"
                    :name="convertImageName(mydata.path)"
                    :path="mydata.path"
                    style="max-width: 210px"
                  ></apollo-image-view>
                </template>
                <template v-else-if="searchType==='object'">
                  <apollo-image-view
                    v-for="mydata in apollo_data"
                    :key="mydata.id"
                    :name="convertImageName(mydata.path)"
                    :probability="Math.round((mydata.detection_score *100))"
                    :path="mydata.path"
                    :bbs="mydata.bb_ymin_xmin_ymax_xmax"
                    style="max-width: 210px"
                  ></apollo-image-view>
                </template>
                <template v-else-if="searchType==='face'">
                  <apollo-image-view
                    v-for="mydata in apollo_data"
                    :key="mydata.id"
                    :name="convertImageName(mydata.path)"
                    :path="mydata.path"
                    :bbs="[[mydata.uly, mydata.ulx, mydata.lry, mydata.lrx]]"
                    style="max-width: 210px"
                  ></apollo-image-view>
                </template>
                 <template v-else-if="searchType==='ner'">
                  <v-card
                    class="mx-auto ma-2"
                    outlined
                    v-for="(mydata, index) in apollo_data" 
                    :key="index">
                    <v-card-text >
                      <div>Found text in </div>
                      <p class="text--primary">
                        {{mydata.snippet}}
                      </p>
                      <div class="text--primary">
                        File: {{mydata.path}}
                      </div>
                      <v-btn color="primary" type="Submit" @click="toggleNeoGraph(searchentity, index)">Details</v-btn>
                      <div v-bind:class="{ neovis: true, open: mydata.open, closed: !mydata.open }" >
                        <div class="graph" :id="'neograph' + index"></div>
                        <analytic-results-view :path="mydata.path" />
                      </div>

                    </v-card-text>
                    
                  </v-card>
                </template>
                <template v-else-if="searchType==='audio/image'">
                  <fulltext-view
                    v-for="mydata in apollo_data_audioocr"
                      :key="mydata.id"
                      :name="convertImageName(mydata.path)"
                      :path="mydata.path"
                      :seconds="mydata.ts_query_timestamps"
                      :snippet="mydata.snippets"
                      :service_name="mydata.service_name"
                      :polygons="mydata.ts_query_polygons"
                      style="max-width: 400px"
                  ></fulltext-view>
                </template>
                <template v-else-if="searchType==='speaker'">
                  <speaker-view
                    v-for="mydata in apollo_data"
                      :key="mydata.path"
                      :path="mydata.path"
                      :prediction="mydata.prediction"
                      :mime_type="mydata.mime_type"
                  ></speaker-view>
                </template>
                <template v-else-if="searchType==='filename'">
                  <result-view
                    :name="convertImageName(fullpath)"
                    :path="fullpath"
                    :dialog_open="true"
                    style="max-width: 210px"
                  ></result-view>
                </template>
                <template v-else-if="searchType==='sentiment'">
                  <fulltext-view
                    v-for="mydata in apollo_data"
                      :key="mydata.id"
                      :path="mydata.path"
                      :service_name="mydata.service_name"
                      :sentiment="mydata.sentiment"
                      :polarity="mydata.polarity"
                      style="max-width: 400px"
                  ></fulltext-view>
                </template>
              </v-row>
            </v-col>
            <!-- End of result ---->
          </v-card>
        </v-col>
      </v-row>
    </v-container>
    <v-spacer></v-spacer>
    <v-spacer></v-spacer>
  </v-main>
</template>


<script>
import ImageView from "./ImageView";
import VideoView from "./VideoView";
import FullTextView from "./FullTextView";
import SpeakerView from "./SpeakerView";
import NeoVis from 'neovis.js/dist/neovis.js';
import AnalyticResultsView from './AnalyticResultsView';
import ResultView from './ResultView';
import vSelect from 'vue-select';
import "vue-select/dist/vue-select.css";

export default { 

  components: {
    "apollo-image-view": ImageView,
    "video-view": VideoView,
    "fulltext-view": FullTextView,
    "speaker-view": SpeakerView,
    "analytic-results-view": AnalyticResultsView,
    "v-select": vSelect,
    "result-view": ResultView,
  },

  name: "Analytics",
  data() {
    return {
      results: [],
      searchfile: null,
      searchtext: "",
      searchtextobject: "",
      searchtextvid: "",
      searchscene: "",
      searchentity:"",
      searchType: "",
      searchfilename: "",
      searchaudio_image:"",
      searchsentiment: "",
      // Setting file and fileLandmark to [] here and in setDefaults avoids "Vue warn Invalid prop: custom validator check failed for prop "value""
      file: [],
      fileLandmark: [],
      fileSpeaker: [], 
      msg: "",
      apollo_data: [],
      apollo_data_audioocr: [],
      neo4j_search_result: "",
      uploaded_files: [],
    };
  },

  methods: {

    /** 
    * Reset the file uploader value for the file input element.
    * This is not necessary with Firefox, but is necessary with Chrome. Without a call to this function, a Chrome user can't search using the same file twice in a row.
    * @param  {string}  element_id The element id (defined in v-file-input), eg 'uploadSpeaker'
    */ 
    doClear: function(element_id) {
      document.querySelector(`#${element_id}`).value='';
    },

    setSearchFile: function(file) {
      this.searchtext = file.name;
    },

    setDefaults: function() {
      console.log('Setting defaults')
      this.results       = []
      this.searchfile    = null
      this.searchtext    = ""
      this.searchtextobject = ""
      this.searchtextvid = ""
      this.searchscene   = ""
      this.searchentity  = ""
      this.searchType    = ""
      this.searchaudio_image = ""
      this.searchfilename = ""
      this.searchsentiment = ""
      this.file          = []
      this.fileLandmark  = []
      this.fileSpeaker = []
      this.msg           = ""
      this.apollo_data   = []
      this.apollo_data_audioocr = []
    },

    toggleNeoGraph(searchtext, index) {
      if (this.apollo_data[index].previously_opened === false) {
        //open for first time, load graph
        this.apollo_data[index].open = true
        this.apollo_data[index].previously_opened = true
        this.drawNeoGraph(searchtext, index)
      }
      else if (this.apollo_data[index].open === true) {
        this.apollo_data[index].open = false
      }
      else {
        this.apollo_data[index].open = true
      }
    },

    drawNeoGraph(searchtext, index) {
    
      //var neov = require('neovis')
      var login = this.getNeo4jLogin()
      var config = {
        encrypted: login.encryption,
        container_id: "neograph" + index,
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

    searchImage: function() {
      this.apollo_data = 0;
      this.msg = "";

      const formData = new FormData();
      formData.append("file", this.file);

      this.apiPut('upload/image?page=0', formData)
        .then(response => response.json())
        .then(data => {
          console.log("searchImage!!!!!! data=" + data);
          console.log(`data.results: ${data.results}`);
          console.log(
            "searchImage!!!!!!  data.faces.length=" + data.results.length
          );

          this.apollo_data = data.results;
          if (data.results.length === 0) this.msg = " 0 faces found";
          else this.msg = " " + data.results.length + " matches found";
          this.searchType = "face";
        })
        .catch(error => {
          console.error(error);
        });

      return this.apollo_data;
    },

    searchLandmark: function() {
      // Clear out any previous landmark-search results and msg
      this.$store.state.pending_data = [] 
      this.$store.state.pending_msg = ""
      this.$store.state.pending_query_data = [this.fileLandmark];
      console.log(
        "Enter searchImage....filename=" +
          this.fileLandmark.name +
          " size=" +
          this.fileLandmark.size +
          " filetype=" +
          this.fileLandmark.type
      );
      const formData = new FormData();
      formData.append("file", this.fileLandmark);

      this.apiPut("upload/landmark?page=0", formData)
        .then(response => response.json())
        .then(data => {
          console.log("searchLandmark!!!!!! data=" + data);
          if (data.landmarks.length == 0) {
            this.msg = data.msg;
            this.$store.state.pending_msg = data.msg;
          } else {
            this.msg = " " + data.landmarks.length + " matches found";
            this.$store.state.pending_msg = this.msg;
            this.apollo_data = data.landmarks;
            this.$store.state.pending_data = data.landmarks;
          }
          this.searchType = "landmark";
        })
        .catch(error => {
          console.error(error);
        });
      return this.apollo_data;
    },

    searchSpeaker: function() {
      console.log(
        "Enter searchImage....filename=" +
          this.fileSpeaker.name +
          " filetype=" +
          this.fileSpeaker.type
      );
      const formData = new FormData();
      formData.append("file", this.fileSpeaker);
      this.apiPut("upload/speaker", formData)
        .then(response => response.json())
        .then(data => {
          console.log("searchSpeaker data=" + data);
          if (data.results == undefined) {
            this.msg = data;
          } else {
            this.msg = " " + data.results.length + " matches found";
            this.apollo_data = data.results;
            let paths = this.apollo_data.map(({path}) => path);
            console.log("searchSpeaker paths" + paths);
          }
          this.searchType = "speaker";
        })
        .catch(error => {
          console.error(error);
        });
      return this.apollo_data;
    },

    searchByFilename: function() {
      this.msg = this.searchfilename;
      this.fullpath = "s3://apollo-source-data/" + this.searchfilename;
      this.searchType = "filename"
    }, 

    searchDatabase: function() {
      console.log("Enter searchDatabase...." + this.searchtextobject);

      if (['faces', 'face'].includes(this.searchtextobject)) {
        this.apiGet("search/facial_recognition")
          .then(response => response.json())
          .then(data => {
            this.apollo_data = data.faces;
            this.msg = this.searchtextobject;
            this.searchType = "face";
          });
      } else {
        this.apiGet("search/tag/?tag=" + this.searchtextobject)
          .then(response => response.json())
          .then(data => {
            console.log(`data: ${data}`)
            console.log(
              "searchDatabase!!!!!!  data.objects.length=" + data.objects.length
            );
            this.apollo_data = data.objects;
            this.msg = this.searchtextobject;
            this.searchType = "object";
          });
      }

      return this.apollo_data;
    },

    searchVidDatabase: function() {
      console.log("Enter searchVidDatabase for " + this.searchtextvid);
      this.apiGet("search/tag_video/?tag=" + this.searchtextvid)
          .then(response => response.json())
          .then(data => {
            console.log(data);
            console.log(
              "searchVidDatabase Object path=" + data.objects[0].path
            );
            console.log(
              "searchVidDatabase data.objects.length=" + data.objects.length
            );
            this.apollo_data = data.objects;
            this.msg = this.searchtext;
            this.searchType = 'video';
          });

      return this.apollo_data;
    },

    searchNamedEntity: function() {
      console.log("Enter searchentity...." + this.searchentity);
      this.apiGet("search/ner/?entity=" + this.searchentity)
        .then(response => response.json())
          .then(data => {
            console.log(
              "searchNamedEntity!!!!!! data =" + data
            );
            this.msg = this.searchentity;
            this.searchType = "ner";
            data.ner_data.forEach(elem => { elem.previously_opened = false; elem.open = false })
            this.apollo_data = data.ner_data;
        });
    },

    searchAudioImage: function() {
      console.log("Enter searchAudioImage...." + this.searchaudio_image);

      //Cloud
      //fetch("https://api.apollo-cttso.com/search/full_text?query=" + this.searchaudio_image, {
      //fetch(this.getApiRootUrl+ "search/full_text?query=" + this.searchaudio_image, {
      this.apiGet("search/full_text?query="  + this.searchaudio_image)
      .then(response => response.json())
        .then(data => {
          console.log(
             "searchAudioImage!!!!!! data =" + data
          );
          console.log(
            "searchAudioImage!!!!!!  data.fulltexts.length =" + data.fulltexts.length
          );

          this.apollo_data_audioocr = data.fulltexts;
          this.msg = this.searchaudio_image;

          this.searchType = "audio/image";
          //this.audioocr_snippet = data.fulltexts[0].snippets
          //this.audioocr_path = data.fulltexts[0].path
      });
    },

    searchScene: function() {
      console.log("Enter searchScene...." + this.searchscene);

      if (this.searchscene == "outdoor" || this.searchscene  ==  "indoor")
      {
        //Cloud
        this.apiGet("search/scene_hierarchy?tag=" + this.searchscene)
          .then(response => response.json())
          .then(data => {
            console.log(
              "searchScene!!!!!!  data.scenes.length=" + data.scenes.length
            );
            this.apollo_data = data.scenes;
            this.msg = this.searchscene;
            this.searchType = "scene";
        });
      }
      else 
      {
        this.apiGet("search/scene_class?tag=" + this.searchscene)
          .then(response => response.json())
          .then(data => {
            console.log(
              "searchScene!!!!!!  data.scenes.length=" + data.scenes.length
            );
            this.apollo_data = data.scenes;
            this.msg = this.searchscene;
            this.searchType = "scene";
        });
      }
      return this.apollo_data;
    },

    searchSentiment() {
        this.apiGet("search/text_sentiment/?sentiment=" + this.searchsentiment)
          .then(response => response.json())
          .then(data => {
            console.log(`data: ${data}`)
            console.log(
              "searchDatabase!!!!!!  data.sentiments.length=" + data.sentiments.length
            );
            this.apollo_data = data.sentiments;
            this.msg = this.searchsentiment;
            this.searchType = "sentiment";
          });
      }
  },

  mounted() {
    this.apiGet("upload/file_list/")
          .then(response => response.json())
          .then(data => {
            var filenames = data.map((filename) => {
              return { name: filename, label: filename.replace("inputs/uploads/", "") }
            })
            this.uploaded_files = filenames.slice(1);
      });
  },

  beforeCreate() {
    //console.log("beforeCreate!!!!!!");
    //this.drawNeoGraph(null);
  },

  beforeMounted() {
    //console.log("beforeMounted!!!!!!");
  }
};
</script>

<style scoped>
  .space
  {
   padding-bottom: 15px;
  }
</style>