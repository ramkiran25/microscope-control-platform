// microscope-metrics.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, interval } from 'rxjs';
import { switchMap } from 'rxjs/operators';

export interface MetricResponse {
  name: string;
  measurements: { statistic: string; value: number }[];
}

@Injectable({ providedIn: 'root' })
export class MicroscopeMetricsService {
  private baseUrl = 'http://localhost:8080/actuator/metrics';

  constructor(private http: HttpClient) {}

  // Poll instrument state every 1 second
  getInstrumentState(): Observable<MetricResponse> {
    return interval(1000).pipe(
      switchMap(() => this.http.get<MetricResponse>(`${this.baseUrl}/microscope.instrument.state`)),
    );
  }
}
