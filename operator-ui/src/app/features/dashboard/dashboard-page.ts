import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { TelemetryService } from '../../core/service/TelemetryService';
import { AcquisitionService } from '../../core/service/Aquistion.Service';
import { TelemetryUpdate } from '../../core/models/models';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.css',
})
export class DashboardPage implements OnInit, OnDestroy {
  telemetry: TelemetryUpdate | null = null;
  justPulsed = false;
  isConnected = false;
  isRunning = false;
  lastActionMessage = '';
  lastActionWasError = false;

  private telemetrySub?: Subscription;
  private connectionSub?: Subscription;

  constructor(
    private telemetryService: TelemetryService,
    private acquisitionService: AcquisitionService,
  ) {}

  ngOnInit(): void {
    this.telemetrySub = this.telemetryService.updates$.subscribe((update: TelemetryUpdate) => {
      this.telemetry = update;
      this.justPulsed = true;
      setTimeout(() => (this.justPulsed = false), 200);
    });

    this.connectionSub = this.telemetryService.connectionStatus$.subscribe((connected: boolean) => {
      this.isConnected = connected;
    });
  }

  ngOnDestroy(): void {
    this.telemetrySub?.unsubscribe();
    this.connectionSub?.unsubscribe();
  }

  runZStack(): void {
    this.isRunning = true;
    this.lastActionMessage = '';
    this.acquisitionService.runZStack().subscribe({
      next: (message: string) => {
        this.isRunning = false;
        this.lastActionMessage = message;
        this.lastActionWasError = false;
      },
      error: (err: HttpErrorResponse) => {
        this.isRunning = false;
        this.lastActionMessage = err.error ?? 'Sequence failed';
        this.lastActionWasError = true;
      },
    });
  }

  resetInstrument(): void {
    this.acquisitionService.reset().subscribe({
      next: (message: string) => {
        this.lastActionMessage = message;
        this.lastActionWasError = false;
      },
      error: (err: HttpErrorResponse) => {
        this.lastActionMessage = err.error ?? 'Reset failed';
        this.lastActionWasError = true;
      },
    });
  }

  get stateClass(): string {
    if (!this.telemetry) return '';
    return 'state-' + this.telemetry.instrumentState.toLowerCase();
  }
  isContinuousRunning = false;

  toggleContinuous(): void {
    const action$ = this.isContinuousRunning
      ? this.acquisitionService.stopContinuous()
      : this.acquisitionService.startContinuous();

    action$.subscribe({
      next: (message: string) => {
        this.isContinuousRunning = !this.isContinuousRunning;
        this.lastActionMessage = message;
        this.lastActionWasError = false;
      },
      error: (err: HttpErrorResponse) => {
        this.lastActionMessage = err.error ?? 'Action failed';
        this.lastActionWasError = true;
      },
    });
  }
}
