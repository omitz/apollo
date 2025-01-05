<template>
<!--
  <div id="app">
    <nav>
      <router-view></router-view>
    </nav>
  </div>
  -->

  <v-app>

    <v-app-bar app color="primary" dark>
      <v-toolbar-title>
        Apollo Command
      </v-toolbar-title>
      <v-spacer/>
      <v-btn
        v-if='this.loggedIn()'
        text
        rounded
        @click="uploadDialog = true"
      >
      Upload
      </v-btn>
      <v-btn 
        v-for="link in links"
        :key=" `${link.label}-header-link` "
        text 
        rounded 
        :to="link.url"
        > 
        {{link.label}}
      </v-btn>
      <v-btn
        v-if='this.loggedIn()'
        text
        rounded
        @click="logout"
      >
      Logout
      </v-btn> 
      <!-- 
        <v-btn @click="toggleTheme" text rounded> Theme  </v-btn>
      -->
    </v-app-bar>

    <v-main>
      <v-dialog
        v-model="uploadDialog"
        width="500"
      >
        <v-card>
          <v-card-title class="headline grey lighten-2">
          Upload a file
          </v-card-title>

          <v-card-text>
            <v-form>
              <v-file-input
                accept="*"
                show-size
                label="Upload file for analysis"
                v-model="uploadFile"
                @click="uploadFile"
              >
              </v-file-input>
              <v-checkbox
                v-model="ignore_hash"
                :label="`ignore file hash`"
              ></v-checkbox>
            </v-form>
          </v-card-text>
          <v-divider></v-divider>

          <v-card-actions>
            <v-spacer></v-spacer>
            <v-btn
              color="secondary"
              text
              @click="uploadDialog = false; uploadFile = []"
            >
              Cancel
            </v-btn>
            <v-btn
              color="primary"
              text
              @click="postMethodToServer"
            >
              Upload
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-dialog>
      <router-view></router-view>
    </v-main>
  <!-- Footer begin -->

  <v-footer
    color="primary lighten-1"
    padless
  >
    <v-row
      justify="center"
      no-gutters
    >
      <v-btn
        v-for="link in links"
        :key=" `${link.label}-footer-link` "
        color="white"
        text
        rounded
        class="my-2"
        :to='link.url'
      >
        {{ link.label }}
      </v-btn>
      <v-col
        class="primary lighten-2 py-4 text-center white--text"
        cols="12"
      >
        {{ new Date().getFullYear() }} — <strong> Apollo Project</strong>
      </v-col>
    </v-row>
  </v-footer>

  <!-- end of footer -->
  </v-app>

</template>

<script>
export default {
  name: 'App',

  components: {
  },
  
  data: () => ({
    //
    showPassword: false,
    links: [],
    uploadDialog: false,
    ignore_hash: false,
    uploadFile: []
  }),

  created () {
    console.log("created!")
    this.setLinks()
  },

  beforeUpdate() {
    console.log("before update!")
    this.setLinks()
  },

  methods: {

    setLinks() {
      if (this.loggedIn()) {

        this.links = [{ label: 'Home', url: '/' }, { label:'Search', url: '/Search' }, { label:'Analytics', url: '/Analytics' }, { label:'Pending', url: '/Pending' }]
        if (this.isAdmin()) {
          this.links.push({ label: 'Admin', url: '/Admin' })
          console.log(this.links)
        }

      }
      else {

        this.links = [ { label: 'Login', url: '/login' } ]

      }

    },

    logout() {
      this.$cookies.set('token', null);
      this.$cookies.set('user', null);
      this.$cookies.set('loggedin', false);
      this.$router.push("login")
      this.$router.go(1)
    },

    toggleTheme(){
      this.$vuetify.theme.dark = !this.$vuetify.theme.dark
    },

    postMethodToServer() {
      console.log(
        "Uploading file... filename=" +
          this.uploadFile.name +
          " size=" +
          this.uploadFile.size +
          " filetype=" +
          this.uploadFile.type
      );

      const formData = new FormData();
      formData.append("file", this.uploadFile);
      formData.append("ignore_hash", this.ignore_hash);

      this.apiPut("upload/", formData)
        .then(response => {
          if (response.status == 200) {
            this.uploadFile = [];
            this.ignore_hash = false;
            alert("file uploaded!");
          }
          else {
            alert("file failed to upload");
          }
        })
    }
  }



};
</script>

<style scoped>

</style>