<template>
  <v-app>
    <v-content> 
    <v-card width="400" class="mx-auto mt-5">

        <v-card-title> <h1 class="display-1"> Login </h1> </v-card-title>
        <v-card-text > 
          <v-form>
            <v-text-field 
              label="Username"
              v-model="username"
              prepend-icon="mdi-account-circle"
            /> 
            <v-text-field 
              :type="showPassword ? 'text' : 'password' "
              label="Password"
              v-model="password"
              prepend-icon="mdi-lock"
              :append-icon="showPassword? 'mdi-eye' : 'mdi-eye-off'"
              @click:append="showPassword=!showPassword"
            />
          </v-form>
        </v-card-text > 

        <v-divider> </v-divider>
        <v-card-actions>
          <v-btn color="success">Register </v-btn>
          <v-spacer>           </v-spacer>
          <v-btn color="info" @click="postLogin">Login</v-btn>
        </v-card-actions>
    </v-card>
    </v-content>
    <!-- End of Login code -->



  </v-app>




</template>S

<script>

export default {
    name: 'Login',
    data () {
      return {
        username: "",
        password: "",
        showPassword: false
      }
    },
    methods: {

      postLogin() {
        var body = { "username": this.username, "password": this.password }
        
        this.apiPost("login/", body)
        .then(response => {

          if (response.status === 401) {
            alert("Incorrect login information")
          }
          else if (response.status !== 200) {
            alert("Error response from server: " + response.status)
          }

          return response.json();
        
        })
        .then(data => {
          if (data.authorization_token) {
            this.$cookies.set('user', data.user);
            this.$cookies.set('token', data.authorization_token);
            this.$cookies.set('loggedin', true);
            this.$router.push("Search")
            this.$router.go(1)
          }
        })
      }
    }
}
</script>

<style scoped>

</style>