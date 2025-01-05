<template>
  <v-main>
    <v-spacer></v-spacer>
    <v-container class="grey lighten-5" id="test" fluid>
      <v-card tile>
        <v-row>
          <v-col cols="12">
            <!--@submit="myFunc" @submit.prevent allows the user to keyboard press Enter instead of mouse clicking Submit (without reloading the page)-->
            <v-form @submit="searchAllAnalytics" @submit.prevent="searchAllAnalytics">
              <v-row>
                <v-col class="file-input-col" col="1" >
                  <v-file-input
                    id="searchfile"
                    accept="image/*,audio/*,video/*"
                    v-model="searchfile"
                    hide-input
                    @change="setSearchFile"
                    @click="setDefaults"
                    v-on:keyup.tab="setDefaults"
                  >
                  </v-file-input>
                </v-col>
                <v-col class="text-input-col" md="10" sm="10" xs="10">
                  <v-combobox 
                    :items="uploaded_files"
                    v-model="searchtext" 
                    label="'Enter search term i.e. laptop, outdoor, wisconsin or upload a file'" 
                    @click="setDefaults" 
                    v-on:keyup.tab="setDefaults"
                    v-on:keyup.enter="onKeyDownSearchBar"
                    ref="search-combobox"
                  >
                  </v-combobox>
                </v-col>
                <v-col class="button-col" md="1" sm="12" xs="12"> 
                  <v-btn class="search-button" color="primary" type="button" @click="onClickSearchButton">Search</v-btn>
                </v-col>
              </v-row>
            </v-form>
          </v-col>
        </v-row>
      </v-card>
      <br />
      <div id="search-results-top"></div>

      <v-card class="pa-2" tile height="auto" min-height="600px">
        <v-row>
          <v-col cols="12">
            <v-card-title>Results: {{msg}}</v-card-title>
            <!-- Display Search Result --->
            <v-row class="white lighten-5" style="height: auto; width:auto; ">
              <!--For each searchType, ':key' should a unique identifier for each element in apollo_data-->
              <template v-if="searchtype==='all'">
                <result-view
                  v-for="(result, index) in results"
                  :key="index"
                  :index="index"
                  :name="convertImageName(result.original_source ? result.original_source : result.path)"
                  :path="result.original_source ? result.original_source : result.path"
                  :dialog_open="false"
                  :bbs="result.bb_ymin_xmin_ymax_xmax ? result.bb_ymin_xmin_ymax_xmax : [[result.uly, result.ulx, result.lry, result.lrx]]"
                  :polygons="result.ts_query_polygons"
                  :seconds="result.ts_query_timestamps ? result.ts_query_timestamps : makeSecondsList(result.seconds)"
                  :snippets="result.snippets"
                  :snippet="result.snippet"
                  :full_text="result.full_text"
                  :service_name="result.service_name"
                  :searchterm="searchtext"
                  style="max-width: 300px"
                ></result-view>
              </template>
              <template v-else-if="searchtype==='filename'">
                <result-view
                  :name="convertImageName(fullpath)"
                  :path="fullpath"
                  :dialog_open="true"
                  style="max-width: 210px"
                >
                </result-view>
              </template>
            </v-row>
            <v-row
              v-if="num_pages > 1"
              align="center"
              justify="center"
            >
              <v-pagination 
                v-model="page_number"
                v-on:input="searchAllAnalytics()"
                :length="num_pages"
                :total-visible="10"
              >
              </v-pagination>
            </v-row>
          </v-col>
        </v-row>
      </v-card>
    </v-container>
    <v-spacer></v-spacer>
    <v-spacer></v-spacer>
  </v-main>
</template>


<script>
import ResultView from './ResultView';

export default {
  components: {
    "result-view": ResultView,
  },

  name: "Search",
  data() {
    return {
      results: [],
      searchfile: null,
      searchtext: "",
      searchtype: "",
      msg: "",
      page_number: 1,
      num_pages: 0,
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
      this.msg           = ""
      this.num_pages     = 0
      this.page_numer    = 1
    },

    files_equal(file1, file2) {
      var path1 = null;
      var path2 = null;
      if (file1) {
        if (file1.original_source) {
          path1 = file1.original_source;
        }
        else if (file1.path) {
          path1 = file1.path
        }
      }
      if (file2) {
        if (file2.original_source) {
          path2 = file2.original_source;

        }
        else if (file2.path) {
          path2 = file2.path
        }
      }
      //I want to keep displaying duplicates for now, so this always returns false
      return path1 === path2 && false;
    },

    save_results: function(service_name, results, num_pages) {
      if (results && results.length > 0) {

        results.forEach((result) => {
          //avoid duplicates w/ same path
          if (!this.results.some(e => this.files_equal(e, result))) {
            this.results.push(result);
          }
        })

      }
      else {
        console.log("no " + service_name + " results")
      }

      if (num_pages > this.num_pages) {
        this.num_pages = num_pages;
      }
    },

    fileIsType(file, type) {
      return file && file['type'].split('/')[0] === type;
    },

    makeSecondsList: function(seconds){
      if (seconds) {
        var secondsList = [];
        var idx;
        for (idx = 0; idx < seconds.length; idx++) {
          secondsList.push(seconds[idx][0])
        }
        return secondsList.sort((a, b) => { return a-b });
      }
      else return [];
    },

    onKeyDownSearchBar: function(keydown_event) {
      if (keydown_event.key === "Enter") {
        this.searchAllAnalytics()
      }
    },

    onClickSearchButton: function() {
      this.$refs['search-combobox'].blur();
      this.$nextTick(() => {
        this.searchAllAnalytics()
      });
    },

    updatePagination: function() {
      this.results = []
      this.searchAllAnalytics()
    },
  
    searchAllAnalytics: function() {
      var searchElement = document.getElementById('search-results-top')
      if (!this.elementIsInViewport(searchElement)) {
        searchElement.scrollIntoView({behavior: "smooth"});
      }
      
      this.results = [];
      this.searchtype = "all"

      if (this.searchfile) {
        this.msg = this.searchfile.name

        if (this.fileIsType(this.searchfile, "image")) {
          
          console.log("input file is image");

          const formData = new FormData();
          formData.append("file", this.searchfile);

          //call facial recognition search
          this.apiPut("upload/image?page=" + (this.page_number - 1), formData)
            .then(response => response.json())
            .then(data => {
              this.save_results("face search", data.results, data.num_pages)
            })
            .catch(error => {
              console.error(error);
            });

          //call landmark search
          this.apiPut("upload/landmark?page=" + (this.page_number - 1), formData)
            .then(response => response.json())
            .then(data => {
              this.save_results("landmark", data.landmarks, data.num_pages)           
            })
            .catch(error => {
              console.error(error);
            });
        }
        else if (this.fileIsType(this.searchfile, "audio") || this.fileIsType(this.searchfile, "video")){
          //call speaker search
          const formData = new FormData();
          formData.append("file", this.searchfile);
          this.apiPut("upload/speaker?page=" + (this.page_number - 1), formData)
            .then(response => response.json())
            .then(data => {
              this.save_results("audio", data.results, data.num_pages)
            })
            .catch(error => {
              console.error(error);
            });
        }
        else {
          alert("filetype " + this.searchfile['type'] + " not supported")
        }
      }
      else if (this.uploaded_files.some(file => { return this.searchtext.value === file.value })) {
        //file search
        this.msg = this.searchtext.text;
        this.fullpath = "s3://apollo-source-data/" + this.searchtext.value;
        this.searchtype = "filename"
      }
      else if (this.searchtext) {
        //call scene search
        if (this.searchtext == "outdoor" || this.searchtext  ==  "indoor")
        {
          this.apiGet("search/scene_hierarchy?tag=" + this.searchtext + "&page=" + (this.page_number - 1))
            .then(response => response.json())
            .then(data => {
              this.save_results("scene", data.scenes, data.num_pages)
            });
        }
        else 
        {
          this.apiGet("search/scene_class?tag=" + this.searchtext + "&page=" + (this.page_number - 1))
            .then(response => response.json())
            .then(data => {
                this.save_results("scene", data.scenes, data.num_pages)
            });
        }

        //call object detection search
        this.apiGet("search/tag/?tag=" + this.searchtext + "&page=" + (this.page_number - 1))
          .then(response => response.json())
            .then(data => {
              this.save_results("object detection", data.objects, data.num_pages)
            });

        //call object detection video search
        this.apiGet("search/tag_video/?tag=" + this.searchtext + "&page=" + (this.page_number - 1))
        .then(response => response.json())
        .then(data => {
          this.save_results("object detection", data.objects, data.num_pages)
        });

        //call fulltext search
        this.apiGet("search/full_text?query="  + this.searchtext + "&page=" + (this.page_number - 1))
          .then(response => response.json())
          .then(data => {
              this.save_results("full-text", data.fulltexts, data.num_pages)
          });

        //call NER search
        this.apiGet("search/ner/?entity=" + this.searchtext)
          .then(response => response.json())
            .then(data => {
              data.ner_data.forEach(elem => { elem.previously_opened = false; elem.open = false })
              this.save_results("NER", data.ner_data, 0)
          });
        
      }
    },
  },

  mounted() {
    this.apiGet("upload/file_list/")
          .then(response => response.json())
          .then(data => {
            var filenames = data.map((filename) => {
              return { value: filename, text: filename.replace("inputs/uploads/", "") }
            })
            this.uploaded_files = filenames.slice(1);
      });
  },

  beforeCreate() {
    //console.log("beforeCreate!!!!!!");
  },

 
};
</script>

<style scoped>

  .file-input-col {
    padding-left: 30px;
    max-width: 60px;
  }

  .text-input-col {
    max-width: 600px;
  }

  .search-button {
    margin-left: 20px;
  }

  .button-col {
    padding-top: 24px;
    align-items: center;
  }
</style>