import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './assets/styles/main.scss'

document.documentElement.classList.add('dark')

createApp(App).use(createPinia()).use(ElementPlus).use(router).mount('#app')
