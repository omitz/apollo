import Vue from 'vue'
import VueRouter from 'vue-router'

import Login from './views/Login.vue'
import Home from './views/Home.vue'
import Explore from './views/Explore.vue'
import ExploreSpeaker from './views/ExploreSpeaker.vue'
import Packages from './views/Packages.vue'
import Upload from './views/Upload.vue'
import FaceMission from './views/FaceMission.vue'
import SpeakerMission from './views/SpeakerMission.vue'
Vue.use(VueRouter)

const router = new VueRouter({
    mode: 'history', //Default mode is Hash mode, 
    routes: [
        {
            path: '/login',
            name: 'Login',
            component: Login
        },
        {
            path: '/', 
            name: 'Home',
            component: Home
        },
        {
            path: '/facemission', 
            name: 'Face Mission Package',
            component: FaceMission
        },
        {
            path: '/speakermission', 
            name: 'Speaker Mission Package',
            component: SpeakerMission
        },

        {
            path: '/explore', 
            name: 'Face Profiles',
            component: Explore
        },
        {
            path: '/explorespeaker', 
            name: 'Speaker Profiles',
            component: ExploreSpeaker
        },

        {
            path: '/upload', 
            name: 'Upload File',
            component: Upload
        },
        {
            path: '/missions', 
            name: 'Mission Packages',
            component: Packages
        },
    ]
})

export default router