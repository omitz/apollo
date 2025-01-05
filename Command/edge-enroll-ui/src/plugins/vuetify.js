import Vue from 'vue';
import Vuetify from 'vuetify/lib';
//import colors from 'vuetify/lib/util/colors';
import { white, black, apollo_blue, apollo_gray, apollo_blue_gray, neon_green } from './colors';
import SearchIcon from '../icons/SearchIcon.vue'
import BackIcon from '../icons/BackIcon.vue'
import JumpBackIcon from '../icons/JumpBackIcon.vue'
import JumpNextIcon from '../icons/JumpNextIcon.vue'
import NextIcon from '../icons/NextIcon.vue'
import AddIcon from '../icons/AddIcon.vue'
import EdgeEnrollIcon from '../icons/EdgeEnrollIcon.vue'


Vue.use(Vuetify);

export default new Vuetify({
    theme: {
        options: { customProperties: true },
        themes: {
          light: {
            primary: white,
            secondary: apollo_gray,
            tertiary: apollo_blue_gray,
            accent: apollo_blue,
            error: '#b71c1c',
            info: '#2196F3',
            success: '#4CAF50',
            warning: '#FFC107',
            text: black,
            active: neon_green,
          },
          dark: {
            primary: '#000',
            secondary: '#000',
            accent: '#000',
            error: '#000',

            'primary--text': '#FFF',
          },
        },
      },
    icons:{
      values: {
        edgeenrollicon: {
          component: EdgeEnrollIcon
        },
        searchicon: {
          component: SearchIcon
        },
        backicon: {
          component: BackIcon
        },
        jumpbackicon: {
          component: JumpBackIcon
        },
        jumpnexticon: {
          component: JumpNextIcon
        },
        nexticon: {
          component: NextIcon
        },
        addicon: {
          component: AddIcon
        },

      },
    },
});


