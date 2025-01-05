<template>
  <v-main>
    <v-container class="grey lighten-5" fluid>
      <v-row>
        <v-col cols="6" md="4">
            <v-card-title>Query image:</v-card-title>
            <v-card-text>{{this.$store.state.pending_query_data[0].name}}</v-card-text>
              <template >
                <apollo-image-view
                  v-for="mydata in this.$store.state.pending_query_data"
                  :key="mydata.name"
                  :name="convertImageName(`inputs/uploads/${mydata.name}`)"
                  :path=mydata.name
                  style="max-width: 210px"
                ></apollo-image-view>
              </template>
        </v-col>
        <v-col cols="12" md="8">
          <v-card>
            <v-card-title>Results: {{this.$store.state.pending_msg}}</v-card-title>
            <!-- Display Search Result --->
            <v-col cols="12">
              <v-row class="white lighten-5" style="height: auto; width:auto; ">
                <template >
                  <apollo-image-view
                    v-for="mydata in this.$store.state.pending_data"
                    :key="mydata.path"
                    :name="convertImageName(mydata.path)"
                    :path=mydata.path
                    style="max-width: 210px"
                  ></apollo-image-view>
                </template>
              </v-row>
            </v-col>
            <!-- End of result ---->
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </v-main>

</template>

<script>
  import ImageView from "./ImageView";
  export default {
    components: {
      "apollo-image-view": ImageView
    },
    name: "Pending"
  }
</script>


<style scoped>

</style>