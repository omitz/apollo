import Vue from 'vue'
import App from './App.vue'
import vuetify from './plugins/vuetify'
import router from './routes'
import { store } from './store.js'
import 'material-design-icons-iconfont/dist/material-design-icons.css'
import VueCookies from 'vue-cookies'
import './assets/css/neovis.css';

Vue.use(VueCookies);
//Vue.use(require('jwt_decode'))
// This disables development warnings
Vue.config.productionTip = false;

// Set global variables
Vue.prototype.$prefixObjDetectLocation = "https://images.apollo-cttso.com/iiif/2/inputs%2F"; //Note: This could be changed to https://images.apollo-cttso.com/iiif/2/. The cantaloupe image server is not limited the 'inputs' directory
Vue.prototype.$suffixObjDetectLocation = "/full/200,/0/default.jpg";
Vue.prototype.$suffixObjDetectLocationLarge = "/full/full/0/default.jpg";

// define functions that all views can use
Vue.mixin({
  methods: {
    // Reference the VUE_APP_ANALYTIC_ENV variable to specify whether to use local or deployed analytic containers
    isAdmin: function() {
      var user = this.$cookies.get('user')
      if (user !== null) {
        if (user.roles.includes('admin')) {
          return true;
        }
        return false;
      }

      return false;
    },

    loggedIn: function() {
      return this.$cookies.get("loggedin") === "true"
    },

    getApiRootUrl: function() {
      if (process.env.VUE_APP_ANALYTIC_ENV === 'local') {
        return 'http://localhost:8080/'
      }
      return 'https://api.apollo-cttso.com/'
    },

    getNeo4jLogin: function() {
      if (process.env.VUE_APP_ANALYTIC_ENV === 'local') {
        return { url: 'neo4j://localhost:7687', username: 'neo4j', password: 'neo4j-password', encryption: 'ENCRYPTION_OFF' }
      }
      var auth = process.env.VUE_APP_NEO4J_AUTH.split("/")
      return { url: 'bolt://neo4j.apollo-cttso.com:7687', username: auth[0], password: auth[1], encryption: 'ENCRYPTION_ON' }
    },

    getBasicHeaders: function() {
      var headers = {
        "Access-Control-Allow-Origin": "http://localhost:*"
      }
      var token = this.$cookies.get('token');
      if (token) {
        headers['Authorization'] = "Bearer " + token
      }
      return headers;
    },

    apiGet: function(url) {
      var headers = this.getBasicHeaders()
      headers.Accept = "application/json"
      headers["Content-Type"] = "application/json"

      return fetch(this.getApiRootUrl() + url, {
        method: "GET",
        headers: headers
      })
    },

    apiPut: function(url, data) {
      var headers = this.getBasicHeaders()

      return fetch(this.getApiRootUrl() + url, {
        method: "PUT",
        body: data,
        headers: headers
      });
    },

    apiPost: function(url, body) {
      var headers = this.getBasicHeaders()
      headers["Content-Type"] = "application/json"

      return fetch(this.getApiRootUrl() + url, {
        method: "POST",
        body: JSON.stringify(body),
        headers: headers
      })
    },

    elementIsInViewport: function(el) {
  
      var rect = el.getBoundingClientRect();
  
      return (
          rect.top >= 0 &&
          rect.left >= 0 &&
          rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) && /* or $(window).height() */
          rect.right <= (window.innerWidth || document.documentElement.clientWidth) /* or $(window).width() */
      );
    },

    /** 
    * Convert an S3 path to a signed url. This hits the flask-apollo-processor's search/get_signed_url endpoint. 
    * The flask-apollo-processor (which has AWS credentials) should return a presigned URL, which allows the UI to access the object for a limited amount of time.
    * @param  {string}  pathToConvert The S3 path. Eg s3://apollo-source-data/inputs/obj_det_vid/utterance_5_book.mp4
    * @returns {string} The signed url.
    */ 
    convertPathToSignedUrl: function(pathToConvert) {
      var bucketName = 'apollo-source-data';
      console.log(
        "Enter convertPathToSignedUrl() ... origin image name = " + pathToConvert
      );
      var keyName = pathToConvert.substring(pathToConvert.lastIndexOf(bucketName)+bucketName.length + 1);
      console.log("keyName: " + keyName);
      var url = `search/get_signed_url/?key=${keyName}`;

      this.apiGet(url)
        .then(response => response.json())
        .then(data => {
          console.log("data=" + data);
          this.signedUrl = data;
        })
        .catch(error => {
          console.error(error);
        });
      return this.signedUrl;
    },
    /** 
    * Get a signed url for the S3 object and open it in a new tab.
    * @param  {string}  pathToConvert The S3 path. Eg s3://apollo-source-data/inputs/obj_det_vid/utterance_5_book.mp4
    */ 
    openInNewTab: function(pathToConvert) {
        var signedurl = this.convertPathToSignedUrl(pathToConvert);
        window.open(signedurl);
    },
    /** 
    * Set the signedUrl variable to the signed url returned by the flask-apollo-processor.
    * @param  {string}  pathToConvert The S3 path. Eg s3://apollo-source-data/inputs/obj_det_vid/utterance_5_book.mp4
    */ 
    setSignedUrl: function (pathToConvert) {
        this.signedUrl = this.convertPathToSignedUrl(pathToConvert);
        console.log(`signedUrl: ${this.signedUrl}`);
    },
    /** 
    * Build the image server string for the small image that gets displayed in the v-card: ${prefix}${filename}${suffix}
    * Reference: https://iiif.io/api/image/3.0/#4-image-requests
    * @param  {string}  filename The S3 path, excluding the bucket and 'inputs' directory. 
    *                             Eg for s3://apollo-source-data/inputs/load_test/annotations/000000000502.jpg this would be load_test%2Fannotations%2F000000000502.jpg
    * @returns {string} The concatenated string. (If there's a "+" character in the filename, we'll replace it with appropriate code: %2B.)
    */ 
    formatThumbnail: function(filename) {
      var formatted = `${this.$prefixObjDetectLocation}${filename}${this.$suffixObjDetectLocation}`.replace("+", "%2B");
      return formatted
    },
    /** 
    * Build the image server string for the large image that gets displayed if a user clicks on the thumbnail.
    * @param  {string}  filename The S3 path, excluding the bucket and 'inputs' directory. 
    * @returns {string} The concatenated string. (If there's a "+" character in the filename, we'll replace it with appropriate code: %2B.)
    */ 
    formatLarge: function(filename) {
      var formatted = `${this.$prefixObjDetectLocation}${filename}${this.$suffixObjDetectLocationLarge}`.replace("+", "%2B");
      return formatted
    },
    /**
     * Use this method to convert the filename to be read from the cantaloupe image server
     * @param {string} name2convert Eg s3://apollo-source-data/inputs/landmark/worcester_000194.jpg
     * @returns {string} convertedFile Eg landmark%2Fworcester_000194.jpg
     */
    convertImageName: function(name2convert) {
      var str = name2convert;
      // console.log(`name to convert: ${name2convert}`)
      var testStr = "inputs/";
      var n = str.lastIndexOf(testStr);
      // console.log(`n: ${n}`)
      var newStr = str.substring(n + testStr.length, str.length);
      // console.log(`newStr: ${newStr}`)
      var convertedFile = newStr.replace(/\//g, "%2F");
      // console.log(`convertedFile: ${convertedFile}`)
      return convertedFile;
    },
  }
});

new Vue({
  vuetify,
  router,
  store,
  render: h => h(App)
}).$mount('#app');

