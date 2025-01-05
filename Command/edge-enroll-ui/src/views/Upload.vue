<template>
    <v-container fluid>
        <v-row justify="space-around">
            <v-col cols="12">
                <h1>Upload images and audio files or search for previously uploaded files</h1>
            </v-col>
        </v-row>
        <v-row justify="space-around">
            <input id="fileUpload" type="file" @change="onFileChange" hidden>
            <v-col cols="3">
                <v-card class="pa-2" tile height="auto" outlined>
                    <v-form>
                    <v-text-field
                        id="fname"
                        v-model="firstname_face"
                        label="First Name"
                        filled
                    ></v-text-field>

                    <v-text-field
                        id="lname"
                        v-model="lastname_face"
                        label="Last Name"
                        filled
                    ></v-text-field>

                    <v-file-input
                        id="uploadFace"
                        accept="image/*"
                        show-size
                        label="Upload face image"
                        v-model="file_face"
                        @click="setDefaults(); doClear('uploadAudio')"
                        v-on:keyup.tab="setDefaults">
                    </v-file-input>


                    <v-file-input
                        id="uploadAudio"
                        accept="audio/*"
                        show-size
                        label="Upload audio"
                        v-model="file_audio"
                        @click="setDefaults(); doClear('uploadFace')"
                        v-on:keyup.tab="setDefaults">
                    </v-file-input>


                    </v-form>      
                    <v-btn type="Submit" @click="uploadImage">Upload image</v-btn>
                </v-card>
            </v-col>
            <v-col cols="3">
<!--                
                <v-card class="pa-2" tile height="auto" outlined>
                    <v-form>
                    <v-text-field
                        id="fname"
                        v-model="firstname_face"
                        label="First Name"
                        filled
                    ></v-text-field>

                    <v-text-field
                        id="lname"
                        v-model="lastname_face"
                        label="Last Name"
                        filled
                    ></v-text-field>

                    <v-file-input
                        id="uploadAudio"
                        accept="image/*"
                        show-size
                        label="Upload audio"
                        v-model="file_face"
                        @click="setDefaults(); doClear('uploadAudio')"
                        v-on:keyup.tab="setDefaults">
                    </v-file-input>
                    
                    </v-form>      
                    <v-btn type="Submit" @click="uploadImage">Upload audio</v-btn>
                </v-card>
-->                
            </v-col>
            <v-col cols="3">
<!--                
                <v-card>
                    <v-card-text>
                        <v-img
                            lazy-src="@/assets/audio.svg"
                            max-height="400"
                            max-width="500"
                            src="@/assets/audio.svg"
                        ></v-img>
                        <p>Search previously uploaded images</p>
                        <v-btn>Search</v-btn>
                    </v-card-text>
                </v-card>
-->                
            </v-col>

        </v-row>
    </v-container>
</template>
<script>
export default {
    name: 'Upload',
    data () {
        return {
            firstname_face: "",
            lastname_face: "",
            file_face: [],
            file_audio: [],                        
            upload_file: {}
        }
    },
    methods: {


        /** 
        * Reset the file uploader value for the file input element.
        * This is not necessary with Firefox, but is necessary with Chrome. Without a call to this function, a Chrome user can't search using the same file twice in a row.
        * @param  {string}  element_id The element id (defined in v-file-input), eg 'uploadSpeaker'
        */ 
        doClear: function(element_id) {
            console.log("Enter doClear");
            document.querySelector(`#${element_id}`).value='';
        },

        setDefaults: function() {
            console.log('Setting defaults')
            //.firstname_img = "",
            //this.lastname_img = "", 
            this.file_face = [];
            this.file_audio = [];          
        },



        uploadImage: function() {
            //this.apollo_data = 0;
            //this.msg = "";

            console.log("Test file_face = "+this.file_face+" file_audio = " + this.file_audio)
            console.log("Enter upload  fname="+this.firstname_face + " lname="+this.lastname_face)

            var vip_name = this.firstname_face+"_"+ this.lastname_face;

            var upload_url;
            const formData = new FormData();

            if(!this.firstname_face || !this.lastname_face)
            {
                alert("Name not filled out")
                return
            }
            
            //Upload face or audio file
            if(this.file_face.length !== 0)
            {
                console.log("Upload: uploading a face image"); 
                formData.append("file", this.file_face);
                upload_url = 'createmodel/vip?analytic=faceid&user=Wole&vip='+vip_name;
            }
            else if (this.file_audio.length !== 0)
            {
                console.log("Upload: uploading a audio file"); 
                formData.append("file", this.file_audio);
                upload_url = 'createmodel/vip?analytic=speakerid&user=Wole&vip='+vip_name;

            }

            this.apiPut(upload_url, formData)
                .then(response => response.json())
                .then(data => {

                console.log("upload file!!!!!! data=" + data);
                //console.log(`data.results: ${data.results}`);
                //console.log(
                //   "uploadImage!!!!!!  data.faces.length=" + data.results.length);

                //this.apollo_data = data.results;
                //if (data.results.length === 0) 
                //    this.msg = " 0 faces found";
                //else 
                //    this.msg = " " + data.results.length + " matches found";
                //this.searchType = "face";
                    alert("File loaded successfully"); 

                })
                .catch(error => {
                    console.error(error);
                });

                //return this.apollo_data;
        },

        chooseFiles: function() {
            document.getElementById("fileUpload").click()
        },
        onFileChange (event) {
            this.upload_file = event.target.files[0];
            console.log(this.upload_file)
        }   
    }
}
</script>

<style scoped>
</style>