import Vue from 'vue'
import VueRouter from 'vue-router'

import Health from './views/Health.vue'
import Login from './views/Login.vue'
import Analytics from './views/Analytics.vue'
import Search from './views/Search.vue'
import Pending from './views/Pending.vue'

import Admin from './views/Admin.vue'

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
            name: 'Login',
            component: Login
        },
        {
            path: '/health', //This is will appended after the domain https://example.com/health
            name: 'Health',
            component: Health
        },
        {
            path: '/analytics', //This is will appended after the domain https://example.com/analytics
            name: 'Analytics',
            component: Analytics //it will load the component Analytics component -  see the import above
        },
        {
            path: '/search',
            name: 'Search',
            component: Search
        },
        {
            path: '/pending',
            name: 'Pending',
            component: Pending
        },
        {
            path: '/admin',
            name: 'Admin',
            component: Admin
        },
    ]
})

export default router