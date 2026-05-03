import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./layout/shell/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'apis' },
      {
        path: 'apis',
        loadComponent: () =>
          import('./features/api-list/api-list.component').then((m) => m.ApiListComponent),
        title: 'API List',
      },
      {
        path: 'builder',
        loadComponent: () =>
          import('./features/api-builder/api-builder.component').then((m) => m.ApiBuilderComponent),
        title: 'API Builder',
      },
      {
        path: 'tester',
        loadComponent: () =>
          import('./features/api-tester/api-tester.component').then((m) => m.ApiTesterComponent),
        title: 'API Tester',
      },
      {
        path: 'pdf',
        loadComponent: () =>
          import('./features/pdf-viewer/pdf-viewer.component').then((m) => m.PdfViewerComponent),
        title: 'PDF Viewer',
      },
      {
        path: 'guide',
        loadComponent: () =>
          import('./features/user-guide/user-guide.component').then((m) => m.UserGuideComponent),
        title: 'User guide',
      },
      {
        path: 'logs',
        loadComponent: () =>
          import('./features/logs-dashboard/logs-dashboard.component').then(
            (m) => m.LogsDashboardComponent,
          ),
        title: 'Logs dashboard',
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
