<template>
<!--
  <div id="app">
    <nav>
      <router-view></router-view>
    </nav>
  </div>
  -->

  <v-app
    color="var(--v-text-base)"
    class="text--lighten-1"
  >

    <v-app-bar app color="var(--v-primary-base)">
      <v-container fluid>
      <v-row class="">
      <span class="app-bar-branding d-flex flex-direction-row">
        <v-icon class="space-after">
          $edgeenrollicon
        </v-icon>
        <v-toolbar-title>
          EDGE <span class="bold">ENROLL</span>
        </v-toolbar-title>
      </span>
      <span>
      <v-btn
        class="app-bar-button" 
        v-for="link in links"
        :key=" `${link.label}-header-link` "
        rounded
        text
        
        :to="link.url"
        > 
        <span class="dot"></span>
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
      </span>
      </v-row>
      </v-container>

      <!-- 
        <v-btn @click="toggleTheme" text rounded> Theme  </v-btn>
      -->
    </v-app-bar>

    <v-main >
      <v-container fluid class="page-container">
            <router-view class="page-body"></router-view>
      </v-container>
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
        {{ new Date().getFullYear() }} — <strong> Apollo Project - Edge Enroll</strong>
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
    this.$vuetify.theme.light = true;
  },

  beforeUpdate() {
    console.log("before update!")
    this.setLinks()
  },

  methods: {

    setLinks() {
      if (this.loggedIn()) {
        console.log("logged in!")
        this.links = [{ label: 'Home', url: '/' }]

      }
      else {
        console.log("logged out!")
        this.links = [{ label: 'Face Profiles', url: '/explore' },
                      { label: 'Speaker Profiles', url: '/explorespeaker' }, 
                      { label: 'Face Mission Package', url: '/facemission' },
                      { label: 'Speaker Mission Package', url: '/speakermission' },
                      { label: 'Upload File', url: '/upload' }, 
                      { label: 'Login', url: '/login' } ]
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
  }

};
</script>

<style scoped>
  .page-container {
    background-color: var(--v-secondary-base)
  }

  .v-icon.space-after {
    margin-right: 15px;
  }
  span.bold {
    font-weight: bold;
  }

  .app-bar-button {
    padding-right: 30px !important;
  }

  .app-bar-button.v-btn--active {
    color: var(--v-accent-base);
  }

  .app-bar-branding {
    margin-right: 150px;
  }

  .dot {
    background-color: var(--v-accent-base);
    height: 11px;
    width: 11px;
    margin-right: 10px;
    border-radius: 50%;
    display: inline-block
  }

  .app-bar-button.app-bar-button.v-btn--active >>> .dot {
    opacity: 1;
  }

  .app-bar-button.app-bar-button >>> .dot {
    opacity: 0;
  }

  .app-bar-button.v-btn--active::before {
    opacity: 0
  }

</style>