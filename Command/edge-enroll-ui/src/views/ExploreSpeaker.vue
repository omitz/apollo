

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
                <v-toolbar-title>List of Persons</v-toolbar-title>
                <v-divider class="mx-4" inset vertical></v-divider>
                <v-spacer></v-spacer>
                <v-dialog v-model="dialog" max-width="500px">
                  <template v-slot:activator="{ on, attrs }">
                    <v-btn
                      color="blue"
                      dark
                      class="mb-2"
                      v-bind="attrs"
                      v-on="on"
                    >
                      Create Speaker Mission
                    </v-btn>
                  </template>
                  <v-card>
                    <v-card-title>
                      <span class="text-h5">{{ formTitle }}</span>
                    </v-card-title>

                    <v-card-text>
                      <v-container>
                        <v-row>
                          <v-col cols="12" sm="6" md="4">
                            <v-text-field
                              v-model="editedItem.name"
                              label="Mission name"
                            ></v-text-field>
                          </v-col>

                          <v-col cols="12" sm="6" md="4">
                            <v-text-field
                              v-model="editedItem.creator"
                              label="Creator"
                            ></v-text-field>
                          </v-col>
                        </v-row>
                      </v-container>
                    </v-card-text>

                    <v-card-actions>
                      <v-spacer></v-spacer>
                      <v-btn color="blue darken-1" text @click="close">
                        Cancel
                      </v-btn>
                      <v-btn
                        color="blue darken-1"
                        text
                        @click="createFaceMission"
                      >
                        Create Mission
                      </v-btn>
                    </v-card-actions>
                  </v-card>
                </v-dialog>
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
      /*      
      profiles: [
        {
          id: 0,
          name: "Abdul Rahman Yasin",
          image: true,
          audio: true,
          date_created: "12/09/2020",
          creator: "Jimmy Swigg",
        },
        {
          id: 1,
          name: "Sherry Lee Kim",
          image: true,
          audio: true,
          date_created: "12/07/2020",
          creator: "Jimmy Swigg",
        },
        {
          id: 2,
          name: "Enrollment Name",
          image: false,
          audio: false,
          date_created: "12/06/2020",
          creator: "Other Creator",
        },
      ],
*/
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

    createFaceMission() {
      /*        
        if (this.editedIndex > -1) {
          Object.assign(this.desserts[this.editedIndex], this.editedItem)
        } else {
          this.desserts.push(this.editedItem)
        }
*/
      console.log("Number of items selected = ", this.selected.length);
      console.log("Mission name = ", this.editedItem.name);

      for (var i = 0; i < this.selected.length; i++) {
        console.log("selected = ", this.selected[i].name);

        var data_url =
          "createmodel/mission?analytic=speakerid&user=Wole&vip=" +
          this.selected[i].name +
          "&mission=" +
          this.editedItem.name;
        //var data_url = 'createmodel/vip?analytic=speakerid&user=Wole&vip=_all_';
        this.apiPut(data_url, null)
          .then((response) => response.json())
          .then((data) => {
            console.log("Adding persons to missions successful=", data.success);
          })
          .catch((error) => {
            console.error(error);
          });
      }

      this.close();

      data_url =
        "createmodel/mission?analytic=speakerid&user=Wole&mission=" +
        this.editedItem.name;
      //var data_url = 'createmodel/vip?analytic=speakerid&user=Wole&vip=_all_';
      this.apiPost(data_url, null)
        .then((response) => response.json())
        .then((data) => {
          console.log("Training missions successful=", data.success);
        })
        .catch((error) => {
          console.error(error);
        });
    },
  },

  created() {
    console.log("Enter created...");
    //curl -X GET "http://localhost:8080/createmodel/vip?analytic=speakerid" -H  "accept: */*"
    var data_url = "createmodel/vip?analytic=speakerid&user=Wole";
    //var data_url = 'createmodel/vip?analytic=speakerid&user=Wole&vip=_all_';
    this.apiGet(data_url)
      .then((response) => response.json())
      .then((data) => {
        console.log(
          "upload file!!!!!! data=" + data.vipContents[0].files[0].name
        );

        for (var i = 0; i < data.vipContents.length; i++) {
          var obj = new Object();
          obj.id = i;
          obj.name = data.vipContents[i].name;
          obj.date_created = "06/22/2021";
          obj.creator = "Wole Omitowoju";
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