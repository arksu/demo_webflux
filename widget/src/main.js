import Vue, { createApp } from '@vue/compat';
import { createPinia } from 'pinia'
import { BootstrapVue, IconsPlugin, ToastPlugin } from 'bootstrap-vue'

import 'bootstrap/dist/css/bootstrap.css';
import 'bootstrap-vue/dist/bootstrap-vue.css';

import './assets/main.css'

import App from './App.vue'
import router from './router'

Vue.use(BootstrapVue)
Vue.use(IconsPlugin)
Vue.use(ToastPlugin)

const app = createApp(App, {
    setup(props, context) {
        console.log(props)
        console.log(context)
    }
})

app.use(createPinia())
app.use(router)

app.mount('#app')
