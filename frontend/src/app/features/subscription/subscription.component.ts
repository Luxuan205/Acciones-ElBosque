import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SubscriptionService } from '../../core/services/subscription.service';
import { SubscriptionStatusResponse, ActivateSubscriptionResponse } from '../../core/models';

@Component({
  selector: 'app-subscription',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './subscription.component.html',
  styleUrls: ['./subscription.component.scss'],
})
export class SubscriptionComponent implements OnInit {
  private subscriptionService = inject(SubscriptionService);

  status = signal<SubscriptionStatusResponse | null>(null);
  loading = signal(true);
  activating = signal(false);
  error = signal<string | null>(null);
  activationResult = signal<ActivateSubscriptionResponse | null>(null);

  benefits = [
    'Comisiones reducidas (0.3% vs 0.5%)',
    'Acceso a órdenes límite y stop-loss',
    'Reportes avanzados de portafolio',
    'Alertas de precio ilimitadas',
  ];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.subscriptionService.getStatus().subscribe({
      next: data => {
        this.status.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el estado de la suscripción.');
        this.loading.set(false);
      },
    });
  }

  activate(): void {
    this.activating.set(true);
    this.error.set(null);
    this.activationResult.set(null);
    this.subscriptionService.activate().subscribe({
      next: result => {
        this.activationResult.set(result);
        this.subscriptionService.getStatus().subscribe({
          next: s => this.status.set(s),
        });
        this.activating.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Error al activar la suscripción.');
        this.activating.set(false);
      },
    });
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('es-CO', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  }
}
