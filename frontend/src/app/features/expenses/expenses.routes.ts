import { Routes } from '@angular/router';

export const EXPENSES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/features/expenses/components/expense-list/expense-list').then(
        (m) => m.ExpenseList,
      ),
  },
];
