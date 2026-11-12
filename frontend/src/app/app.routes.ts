import { Routes } from '@angular/router';
import { authGuard, studentGuard, teacherGuard } from './core/guards/auth.guard';
import { AppShell } from './shared/layout/app-shell';

/** Rotas da aplicacao com carregamento tardio por area (RNF26). */
export const routes: Routes = [
  {
    path: 'entrar',
    loadComponent: () => import('./features/auth/login.page').then((module) => module.LoginPage),
    title: 'Entrar · Educa Mais',
  },
  {
    path: 'cadastrar',
    loadComponent: () => import('./features/auth/register.page').then((module) => module.RegisterPage),
    title: 'Criar conta · Educa Mais',
  },
  {
    path: '',
    component: AppShell,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'aluno' },
      {
        path: 'onboarding',
        loadComponent: () => import('./features/onboarding/interests.page').then((module) => module.InterestsPage),
        title: 'Seus interesses · Educa Mais',
      },
      {
        path: 'aluno',
        canActivate: [studentGuard],
        children: [
          {
            path: '',
            loadComponent: () => import('./features/student/journey.page').then((module) => module.JourneyPage),
            title: 'Minha jornada · Educa Mais',
          },
          {
            path: 'missoes',
            loadComponent: () => import('./features/student/missions.page').then((module) => module.MissionsPage),
            title: 'Missoes · Educa Mais',
          },
          {
            path: 'missoes/:missionId',
            loadComponent: () => import('./features/student/mission.page').then((module) => module.MissionPage),
            title: 'Missao de recuperacao · Educa Mais',
          },
          {
            path: 'loja',
            loadComponent: () => import('./features/shop/shop.page').then((module) => module.ShopPage),
            title: 'Loja do avatar · Educa Mais',
          },
          {
            path: 'perfil',
            loadComponent: () => import('./features/student/profile.page').then((module) => module.ProfilePage),
            title: 'Perfil · Educa Mais',
          },
        ],
      },
      {
        path: 'professor',
        canActivate: [teacherGuard],
        loadComponent: () => import('./features/teacher/dashboard.page').then((module) => module.DashboardPage),
        title: 'Dashboard da turma · Educa Mais',
      },
    ],
  },
  { path: '**', redirectTo: 'aluno' },
];
