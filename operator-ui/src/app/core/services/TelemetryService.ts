import { Injectable, OnDestroy } from "@angular/core";
import { Observable, Subject } from "rxjs";
import { TelemetryUpdate } from "../models/models";

@Injectable({ providedIn: "root" })
export class TelemetryService implements OnDestroy {
  private readonly wsUrl = "ws://localhost:8080/ws/telemetry";
  private readonly reconnectDelayMs = 2000;

  private socket: WebSocket | null = null;
  private readonly updatesSubject = new Subject<TelemetryUpdate>();
  private readonly connectionStatusSubject = new Subject<boolean>();
  private shouldReconnect = true;

  /** Live telemetry stream — subscribe to this from components. */
  readonly updates$: Observable<TelemetryUpdate> =
    this.updatesSubject.asObservable();

  /** Emits true when connected, false when disconnected . */
  readonly connectionStatus$: Observable<boolean> =
    this.connectionStatusSubject.asObservable();

  constructor() {
    this.connect();
  }

  private connect(): void {
    this.socket = new WebSocket(this.wsUrl);

    this.socket.onopen = () => {
      this.connectionStatusSubject.next(true);
    };

    this.socket.onmessage = (event: MessageEvent<string>) => {
      try {
        const update: TelemetryUpdate = JSON.parse(event.data);
        this.updatesSubject.next(update);
      } catch {
        // Malformed message — ignore rather than crash the stream.
      }
    };

    this.socket.onclose = () => {
      this.connectionStatusSubject.next(false);
      if (this.shouldReconnect) {
        setTimeout(() => this.connect(), this.reconnectDelayMs);
      }
    };

    this.socket.onerror = () => {
      this.socket?.close();
    };
  }

  ngOnDestroy(): void {
    this.shouldReconnect = false;
    this.socket?.close();
  }
}
