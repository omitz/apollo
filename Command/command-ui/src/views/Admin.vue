<template>
    <v-content>
        <v-spacer></v-spacer>
        <v-container class="grey lighten-5 mx-auto mt-5" fluid>
            <v-card width="600" class="mx-auto mt-5">
                <v-card-text>
                    <v-row class="px-auto" align="center" justify="center">
                        <v-col class="px-auto" cols="12">
                            <v-btn class="px-auto" color="primary" type="Submit" @click="restoreDialog = true">Restore Databases</v-btn>
                        </v-col>
                    </v-row>
                    <v-row align="center" justify="center">
                        <v-col cols="12">
                            <v-btn color="primary" type="Submit" @click="fixturesDialog = true">Create database fixtures</v-btn>
                        </v-col>
                    </v-row>
                    <v-row align="center" justify="center">
                        <v-col cols="12">
                            <v-btn color="primary" type="Submit" @click="newUserDialog = true">Create new user</v-btn>
                        </v-col>
                    </v-row>
                </v-card-text>
            </v-card>
        </v-container>
        <v-dialog
            v-model="restoreDialog"
            width="500"
        >
            <v-card>
                <v-card-title class="headline grey lighten-2">
                Restore Databases?
                </v-card-title>

                <v-divider></v-divider>

                <v-card-actions>
                <v-spacer></v-spacer>
                <v-btn
                    color="secondary"
                    text
                    @click="restoreDialog = false"
                >
                    Cancel
                </v-btn>
                <v-btn
                    color="primary"
                    text
                    @click="restoreDatabases"
                >
                    Yes
                </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
        <v-dialog
            v-model="fixturesDialog"
            width="500"
        >
            <v-card>
                <v-card-title class="headline grey lighten-2">
                Create New Database fixtures?
                </v-card-title>
                <v-card-text>
                    This will replace the current database fixtures in S3.
                </v-card-text>
                <v-divider></v-divider>

                <v-card-actions>
                <v-spacer></v-spacer>
                <v-btn
                    color="secondary"
                    text
                    @click="fixturesDialog = false"
                >
                    Cancel
                </v-btn>
                <v-btn
                    color="primary"
                    text
                    @click="createDatabaseFixtures"
                >
                    Yes
                </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
        <v-dialog
            v-model="newUserDialog"
            width="500"
        >
            <v-card>
                <v-card-title class="headline grey lighten-2">
                New User
                </v-card-title>

                <v-divider></v-divider>
                <v-card-text>
                    <v-form>
                        <v-text-field
                            v-model="newUser.username"
                            label="username"
                            autocomplete="new-password"
                        />
                        <v-text-field 
                            :type="showPassword ? 'text' : 'password' "
                            label="Password"
                            v-model="newUser.password"
                            prepend-icon="mdi-lock"
                            :append-icon="showPassword ? 'mdi-eye' : 'mdi-eye-off'"
                            @click:append="showPassword=!showPassword"
                            autocomplete="new-password"
                        />
                        <v-checkbox
                            v-model="newUser.admin"
                            :label="`admin access`"
                        ></v-checkbox>
                    </v-form>
                </v-card-text>
                <v-card-actions>
                <v-spacer></v-spacer>
                <v-btn
                    color="secondary"
                    text
                    @click="cancelNewUser"
                >
                    Cancel
                </v-btn>
                <v-btn
                    color="primary"
                    text
                    @click="submitNewUser"
                >
                    Submit
                </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
    </v-content>
</template>
<script>
export default {
  components: {
  },

  name: "Admin",
  data() {
    return {
        restoreDialog: false,
        fixturesDialog: false,
        newUserDialog: false,
        
        newUser: {
            username: "",
            password: "",
            admin: false
        },
        showPassword: false
    };
  },

  methods: {
    submitNewUser: function() {
        var user = {
            username: this.newUser.username,
            password: this.newUser.password
        }

        if (this.newUser.admin) {
            user.roles = ["admin", "user"]
        }
        else {
            user.roles = ["user"]
        }
        
        this.apiPost("users/", user).then(response => {
            if (response.status === 200) {
                alert("Account created")
            }
            else {
                alert("failure")
                console.log(response.json())
            }
        })
    },

    cancelNewUser: function() {
        this.newUserDialog = false;
        this.newUser = {
            username: "",
            password: "",
            admin: false
        }
    },

    restoreDatabases: function() {
      this.apiGet("admin/restore_databases")
        .then(response => response.json()).then(result => {
            if (result.success) {
                alert("databases restored")
            }
            else {
                alert("database restore failed")
            }
        })
        this.restoreDialog = false;
    },

    createDatabaseFixtures: function() {
        this.apiGet("admin/create_fixtures")
        .then(response => response.json()).then(result => {
            if (result.success) {
                alert("database fixtures created")
            }
            else {
                alert("database fixtures create failed")
            }
        })
        this.fixturesDialog = false;
    },
  }
};
</script>

<style scoped>

</style>  