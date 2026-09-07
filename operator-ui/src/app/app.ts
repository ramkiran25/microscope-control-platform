import { Component } from "@angular/core";
import { DashboardPage } from "./features/dashboard/dashboard-page";

@Component({
  selector: "app-root",
  standalone: true,
  imports: [DashboardPage],
  template: `<app-dashboard-page></app-dashboard-page>`,
})
export class App {}
