import {createRouter, createWebHistory} from 'vue-router'
import WidgetView from '../views/WidgetView.vue'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/invoice/:id',
            name: 'invoice',
            component: WidgetView
        },
        {
            path: '/:id',
            name: 'widget',
            component: WidgetView
        }
    ]
})

export default router
