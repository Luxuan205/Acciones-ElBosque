import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { NotificationService } from '../../../core/services/notification.service';
import { MarketAlertSubscriptionDto } from '../../../core/models';

@Component({
  selector: 'app-market-alerts',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './market-alerts.component.html',
  styleUrls: ['./market-alerts.component.scss'],
})
export class MarketAlertsComponent implements OnInit {
  private notificationService = inject(NotificationService);
  private fb = inject(FormBuilder);

  alerts = signal<MarketAlertSubscriptionDto[]>([]);
  loading = signal(true);
  submitting = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  confirmDeleteId = signal<number | null>(null);

  alertTypes = ['MARKET_OPEN', 'MARKET_CLOSE', 'HIGH_VOLATILITY', 'LARGE_MOVEMENT'];

  form = this.fb.group({
    alertType: ['', Validators.required],
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.notificationService.getMarketAlerts().subscribe({
      next: data => {
        this.alerts.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las alertas de mercado.');
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid) return;
    this.submitting.set(true);
    this.error.set(null);
    this.success.set(null);
    this.notificationService.createMarketAlert({ alertType: this.form.value.alertType! }).subscribe({
      next: created => {
        this.alerts.update(list => [created, ...list]);
        this.form.reset();
        this.success.set('Alerta de mercado creada exitosamente.');
        this.submitting.set(false);
      },
      error: () => {
        this.error.set('Error al crear la alerta de mercado.');
        this.submitting.set(false);
      },
    });
  }

  requestDelete(id: number): void {
    this.confirmDeleteId.set(id);
  }

  cancelDelete(): void {
    this.confirmDeleteId.set(null);
  }

  confirmDelete(id: number): void {
    this.notificationService.deleteMarketAlert(id).subscribe({
      next: () => {
        this.alerts.update(list => list.filter(a => a.id !== id));
        this.confirmDeleteId.set(null);
      },
      error: () => {
        this.error.set('Error al eliminar la alerta.');
        this.confirmDeleteId.set(null);
      },
    });
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('es-CO', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }
}
