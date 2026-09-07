import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AcquisitionService {
  private readonly baseUrl = 'http://localhost:8080/acquisition';

  constructor(private http: HttpClient) {}

  runZStack(): Observable<string> {
    return this.http.post(`${this.baseUrl}/z-stack`, null, {
      responseType: 'text',
    });
  }

  reset(): Observable<string> {
    return this.http.post(`${this.baseUrl}/reset`, null, {
      responseType: 'text',
    });
  }
  startContinuous(): Observable<string> {
    return this.http.post(`${this.baseUrl}/continuous/start`, null, { responseType: 'text' });
  }

  stopContinuous(): Observable<string> {
    return this.http.post(`${this.baseUrl}/continuous/stop`, null, { responseType: 'text' });
  }
}
