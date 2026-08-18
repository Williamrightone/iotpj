import { createRouter, createWebHistory } from 'vue-router'
import LoginView           from '../views/LoginView.vue'
import DashboardView       from '../views/DashboardView.vue'
import UsersView           from '../views/UsersView.vue'
import FeaturesView        from '../views/FeaturesView.vue'
import RolePermissionsView from '../views/RolePermissionsView.vue'
import ProcessConfigView   from '../views/ProcessConfigView.vue'
import StationDetailView   from '../views/StationDetailView.vue'

const routes = [
  { path: '/',                        redirect: '/dashboard' },
  { path: '/login',                   name: 'Login',           component: LoginView },
  { path: '/dashboard',               name: 'Dashboard',       component: DashboardView,       meta: { auth: true } },
  { path: '/users',                   name: 'Users',           component: UsersView,           meta: { auth: true, adminOnly: true } },
  { path: '/features',                name: 'Features',        component: FeaturesView,        meta: { auth: true } },
  { path: '/role-permissions',        name: 'RolePermissions', component: RolePermissionsView, meta: { auth: true } },
  { path: '/process-config',          name: 'ProcessConfig',   component: ProcessConfigView,   meta: { auth: true } },
  { path: '/process-config/:id',      name: 'StationDetail',   component: StationDetailView,   meta: { auth: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const token = localStorage.getItem('accessToken')
  if (to.meta.auth && !token) return { name: 'Login' }
  if (to.name === 'Login' && token) return { name: 'Dashboard' }
})

export default router
