

<style scoped>
.filters-panel {
  background-color: var(--v-tertiary-base);
}
.v-input >>> .v-label {
  font-style: italic;
}
.v-btn.lower-case {
  text-transform: initial;
}
.margin-right {
  margin-right: 15px;
}
</style>

<template>
  <v-row>
    <v-col cols="12">
      <v-row>
        <v-col cols="3">
          <v-autocomplete
            background-color="primary"
            v-model="search_profile"
            :items="profiles"
            prepend-inner-icon="$searchicon"
            item-text="name"
            item-value="id"
            label="Search Profile"
            filled
            rounded
            dense
          ></v-autocomplete>
        </v-col>
        <v-spacer />
        <v-col cols="3">
          <!--          
          <v-btn class="lower-case" color="accent">
            Create Face Enrollment
          </v-btn>
-->
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="12">
          <!--          
          <v-data-table
            :headers="headers"
            :items="profiles"
            :items-per-page="25"
            item-key="name"
            class="elevation-1"
            show-select
            :footer-props="{
              showFirstLastPage: true,
              firstIcon: '$jumpbackicon',
              lastIcon: '$jumpnexticon',
              prevIcon: '$backicon',
              nextIcon: '$nexticon',
            }"
          >
            <template v-slot:item.image="{ item }">
              <div v-if="item.image" style="font-style: italic">
                <v-icon class="margin-right">$addicon</v-icon>uploaded
              </div>
              <div v-else>
                <v-icon class="margin-right">$addicon</v-icon>
              </div>
            </template>
            <template v-slot:item.audio="{ item }">
              <div v-if="item.audio" style="font-style: italic">
                <v-icon class="margin-right">$addicon</v-icon>uploaded
              </div>
              <div v-else>
                <v-icon class="margin-right">$addicon</v-icon>
              </div>
            </template>
          </v-data-table>
-->
          <v-data-table
            v-model="selected"
            :headers="headers"
            :items="profiles"
            sort-by="name"
            class="elevation-1"
            show-select
            item-key="name"
          >
            <template v-slot:top>
              <v-toolbar flat>
                <v-toolbar-title>Face Mission Packages</v-toolbar-title>
                <v-divider class="mx-4" inset vertical></v-divider>
                <v-spacer></v-spacer>
                     <v-btn
                      color="blue"
                      dark
                      class="mb-2"
                      @click="downloadMission"
                    >
                      Download Speaker Mission
                    </v-btn>

                <v-dialog v-model="dialogDelete" max-width="500px">
                  <v-card>
                    <v-card-title class="text-h5"
                      >Are you sure you want to delete this item?</v-card-title
                    >
                    <v-card-actions>
                      <v-spacer></v-spacer>
                      <v-btn color="blue darken-1" text @click="closeDelete"
                        >Cancel</v-btn
                      >
                      <v-btn
                        color="blue darken-1"
                        text
                        @click="deleteItemConfirm"
                        >OK</v-btn
                      >
                      <v-spacer></v-spacer>
                    </v-card-actions>
                  </v-card>
                </v-dialog>
              </v-toolbar>
            </template>
            <template v-slot:item.actions="{ item }">
              <v-icon small class="mr-2" @click="editItem(item)">
                mdi-pencil
              </v-icon>
              <v-icon small @click="deleteItem(item)"> mdi-delete </v-icon>
            </template>
            <template v-slot:no-data>
              <v-btn color="primary" @click="initialize"> Reset </v-btn>
            </template>
          </v-data-table>
        </v-col>
      </v-row>
    </v-col>
  </v-row>
</template>

<script>
export default {
  name: "Explore",
  data() {
    return {
      search_profile: {},
      dialog: false,
      dialogDelete: false,
      selected: [],
      headers: [
        { text: "Name", align: "start", value: "name", sortable: false },
        /*        { text: "Image", value: "image", align: "start" },
        { text: "Audio", value: "audio", align: "start" },
*/
        { text: "Date Created", value: "date_created", align: "start" },
        //{ text: "Creator", value: "creator", align: "start" },
        { text: "Actions", value: "actions", sortable: false },
      ],
      editedIndex: -1,
      editedItem: {
        name: "",
        date_created: 0,
        creator: "apollo_user",
      },
      defaultItem: {
        name: "",
        date_created: 0,
        creator: "apollo_user",
      },

      profiles: [],
    };
  },

  computed: {
    formTitle() {
      return this.editedIndex === -1 ? "New Mission" : "Edit Item";
    },
  },

  watch: {
    dialog(val) {
      val || this.close();
    },
    dialogDelete(val) {
      val || this.closeDelete();
    },
  },

  methods: {
    initialize() {},

    editItem(item) {
      this.editedIndex = this.profiles.indexOf(item);
      this.editedItem = Object.assign({}, item);
      this.dialog = true;
    },

    deleteItem(item) {
      console.log("Enter deleteItem = name", item.name);
      this.editedIndex = this.profiles.indexOf(item);
      this.editedItem = Object.assign({}, item);
      this.dialogDelete = true;
    },

    deleteItemConfirm() {
      this.profiles.splice(this.editedIndex, 1);
      this.closeDelete();
    },

    close() {
      this.dialog = false;
      this.$nextTick(() => {
        this.editedItem = Object.assign({}, this.defaultItem);
        this.editedIndex = -1;
      });
    },

    closeDelete() {
      console.log("Enter closeDelete = name", this.defaultItem.name);
      this.dialogDelete = false;
      this.$nextTick(() => {
        this.editedItem = Object.assign({}, this.defaultItem);
        console.log("closeDelete():  name=", this.editedItem.name);
        this.editedIndex = -1;
      });
    },

    downloadMission() {

      console.log("Number of items selected = ", this.selected.length);

      for (var i = 0; i < this.selected.length; i++) {
        console.log("selected = ", this.selected[i].name);
        var data_url =
          "createmodel/mission/download?analytic=speakerid&user=Wole&mission="+
          this.selected[i].name + "&download=dataset"

        //var data_url = 'createmodel/vip?analytic=faceid&user=Wole&vip=_all_';
        this.apiGet(data_url)
          .then((response) =>response.blob())
          .then((blob) => {
            console.log("Download speaker missions successful=");

            //var header = response.getResponseHeader('Content-Disposition');
            //console.log(response);

            var url = window.URL.createObjectURL(blob);
            window.location.assign(url);
   
            //var a = document.createElement('a');
            //a.href = url;
            //a.download = "speaker.zip";
            //document.body.appendChild(a); // we need to append the element to the dom -> otherwise it will not work in firefox
            //a.click();    
            //a.remove();  //afterwards we remove the element again 
          })
          .catch((error) => {
            console.error(error);
          });
      }

    },
  },

  created() {
    console.log("Enter created...");
    var data_url = 'createmodel/mission?analytic=speakerid&user=Wole';

    this.apiGet(data_url)
      .then((response) => response.json())
      .then((data) => {
        console.log("Getting all missions data=" + data.missions[0]);

        for(var i = 0; i < data.missions.length; i++)
        {
            var obj = new Object();
            obj.id = i;
            obj.name = data.missions[i];
            obj.date_created ="06/22/2021";
            obj.image = false; 
            obj.download - false;
            obj.creator = "Wole Omitowoju"
            this.profiles.push(obj);
        }
        //alert("File loaded successfully");
      })
      .catch((error) => {
        console.error(error);
      });
  },
};
</script>