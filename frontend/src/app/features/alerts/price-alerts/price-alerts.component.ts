import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { NotificationService } from '../../../core/services/notification.service';
import { PriceAlertDto } from '../../../core/models';

@Component({
  selector: 'app-price-alerts',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './price-alerts.component.html',
  styleUrls: ['./price-alerts.component.scss'],
})
export class PriceAlertsComponent implements OnInit {
  private notificationService = inject(NotificationService);
  private fb = inject(FormBuilder);

  alerts = signal<PriceAlertDto[]>([]);
  loading = signal(true);
  submitting = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);

  form = this.fb.group({
    symbol: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(10)]],
    alertType: ['ABSOLUTE', Validators.required],
    threshold: [null as number | null, [Validators.required, Validators.min(0.01)]],
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.notificationService.getPriceAlerts().subscribe({
      next: data => {
        this.alerts.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las alertas de precio.');
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid) return;
    this.submitting.set(true);
    this.error.set(null);
    this.success.set(null);
    const { symbol, alertType, threshold } = this.form.value;
    this.notificationService
      .createPriceAlert({ symbol: symbol!, alertType: alertType!, threshold: threshold! })
      .subscribe({
        next: created => {
          this.alerts.update(list => [created, ...list]);
          this.form.reset({ alertType: 'ABSOLUTE' });
          this.success.set('Alerta de precio creada exitosamente.');
          this.submitting.set(false);
        },
        error: () => {
          this.error.set('Error al crear la alerta de precio.');
          this.submitting.set(false);
        },
      });
  }

  toggle(alert: PriceAlertDto): void {
    const isActive = alert.status === 'ACTIVE';
    const obs = isActive
      ? this.notificationService.deactivatePriceAlert(alert.id)
      : this.notificationService.reactivatePriceAlert(alert.id);
    obs.subscribe({
      next: updated => {
        this.alerts.update(list =>
          list.map(a => (a.id === alert.id ? updated : a))
        );
      },
      error: () => {
        this.error.set('Error al cambiar el estado de la alerta.');
      },
    });
  }

  delete(id: number): void {
    this.notificationService.deletePriceAlert(id).subscribe({
      next: () => {
        this.alerts.update(list => list.filter(a => a.id !== id));
      },
      error: () => {
        this.error.set('Error al eliminar la alerta.');
      },
    });
  }

  statusBadge(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'badge-green';
      case 'TRIGGERED': return 'badge-cyan';
      case 'INACTIVE': return 'badge-red';
      case 'SUSPENDED': return 'badge-red';
      default: return 'badge-yellow';
    }
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('es-CO', {
      timeZone: 'America/Bogota',
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }
}
