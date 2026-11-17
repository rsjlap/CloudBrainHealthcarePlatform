import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { guest: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/auth/ProfileView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'doctor/inspector',
          name: 'doctor-inspector',
          component: () => import('@/views/doctor/InspectorQueueView.vue'),
          meta: { requiresAuth: true, roles: ['INSPECTOR'] },
        },
        {
          path: 'doctor/inspector/history',
          name: 'doctor-inspector-history',
          component: () => import('@/views/doctor/InspectorHistoryView.vue'),
          meta: { requiresAuth: true, roles: ['INSPECTOR'] },
        },
        {
          path: 'doctor/pharmacist',
          name: 'doctor-pharmacist',
          component: () => import('@/views/doctor/PharmacistDispenseView.vue'),
          meta: { requiresAuth: true, roles: ['PHARMACIST'] },
        },
        {
          path: 'doctor/pharmacist/history',
          name: 'doctor-pharmacist-history',
          component: () => import('@/views/doctor/PharmacistHistoryView.vue'),
          meta: { requiresAuth: true, roles: ['PHARMACIST'] },
        },
      ],
    },
    {
      path: '/doctor',
      component: () => import('@/layouts/DoctorLayout.vue'),
      meta: { requiresAuth: true, roles: ['DOCTOR'] },
      children: [
        {
          path: '',
          name: 'doctor',
          component: () => import('@/views/doctor/DoctorWorkbenchView.vue'),
          meta: { requiresAuth: true, roles: ['DOCTOR'] },
        },
        {
          path: 'history',
          name: 'doctor-history',
          component: () => import('@/views/doctor/DoctorMedicalHistoryView.vue'),
          meta: { requiresAuth: true, roles: ['DOCTOR'] },
        },
        {
          path: 'profile',
          name: 'doctor-profile',
          component: () => import('@/views/doctor/DoctorProfileView.vue'),
          meta: { requiresAuth: true, roles: ['DOCTOR'] },
        },
        {
          path: 'medical-record',
          redirect: '/doctor',
        },
        {
          path: 'prescription',
          redirect: '/doctor',
        },
      ],
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] },
      children: [
        { path: '', redirect: '/admin/dashboard' },
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: () => import('@/views/admin/AdminDashboardView.vue'),
        },
        {
          path: 'users',
          component: () => import('@/views/admin/users/AdminUsersLayout.vue'),
          redirect: '/admin/users/patients',
          children: [
            {
              path: 'patients',
              name: 'admin-users-patients',
              component: () => import('@/views/admin/users/AdminPatientsView.vue'),
            },
            {
              path: 'staff',
              name: 'admin-users-staff',
              component: () => import('@/views/admin/users/AdminStaffView.vue'),
            },
          ],
        },
        {
          path: 'schedules',
          name: 'admin-schedules',
          component: () => import('@/views/admin/AdminSchedulesView.vue'),
        },
        {
          path: 'equipment',
          name: 'admin-equipment',
          component: () => import('@/views/admin/AdminEquipmentView.vue'),
        },
        {
          path: 'drugs',
          name: 'admin-drugs',
          component: () => import('@/views/admin/AdminDrugsView.vue'),
        },
        {
          path: 'registrations',
          name: 'admin-registrations',
          component: () => import('@/views/admin/AdminRegistrationsView.vue'),
        },
      ],
    },
  ],
})

function staffHome(role: string | undefined) {
  if (role === 'ADMIN') return { name: 'admin-dashboard' }
  if (role === 'DOCTOR') return { name: 'doctor' }
  if (role === 'INSPECTOR') return { name: 'doctor-inspector' }
  if (role === 'PHARMACIST') return { name: 'doctor-pharmacist' }
  return { name: 'home' }
}

router.beforeEach((to) => {
  const userStore = useUserStore()

  if (!userStore.isLoggedIn) {
    if (to.meta.requiresAuth) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
    return true
  }

  if (userStore.userInfo?.role === 'PATIENT') {
    userStore.logout()
    return { name: 'login', query: { reason: 'patient-disabled' } }
  }

  if (to.meta.guest) {
    return staffHome(userStore.userInfo?.role)
  }

  if (userStore.isAdmin && !to.path.startsWith('/admin')) {
    return { name: 'admin-dashboard' }
  }

  const roles = to.meta.roles as string[] | undefined
  if (roles && userStore.userInfo && !roles.includes(userStore.userInfo.role)) {
    return staffHome(userStore.userInfo.role)
  }

  return true
})

export default router
