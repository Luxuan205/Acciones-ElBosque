import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../../core/services/admin.service';
import {
  OperationalMetricsDto,
  FinancialSummaryDto,
  AdminLinkDto
} from '../../../core/models';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss'
})
export class AdminDashboardComponent implements OnInit {
  private readonly adminSvc = inject(AdminService);

  metrics = signal<OperationalMetricsDto | null>(null);
  financial = signal<FinancialSummaryDto | null>(null);
  links = signal<AdminLinkDto[]>([]);
  loadingMetrics = signal(true);
  loadingFinancial = signal(true);
  loadingLinks = signal(true);
  metricsError = signal<string | null>(null);
  financialError = signal<string | null>(null);
  selectedPeriod = signal<string>('TODAY');

  periods = ['TODAY', 'WEEK', 'MONTH'];

  ngOnInit(): void {
    this.loadMetrics();
    this.loadFinancial();
    this.loadLinks();
  }

  loadMetrics(): void {
    this.loadingMetrics.set(true);
    this.adminSvc.getDashboard().subscribe({
      next: data => { this.metrics.set(data); this.loadingMetrics.set(false); },
      error: () => { this.metricsError.set('Error al cargar métricas operacionales.'); this.loadingMetrics.set(false); }
    });
  }

  loadFinancial(): void {
    this.loadingFinancial.set(true);
    this.adminSvc.getFinancialSummary(this.selectedPeriod()).subscribe({
      next: data => { this.financial.set(data); this.loadingFinancial.set(false); },
      error: () => { this.financialError.set('Error al cargar resumen financiero.'); this.loadingFinancial.set(false); }
    });
  }

  loadLinks(): void {
    this.adminSvc.getAdminLinks().subscribe({
      next: data => { this.links.set(data); this.loadingLinks.set(false); },
      error: () => this.loadingLinks.set(false)
    });
  }

  onPeriodChange(period: string): void {
    this.selectedPeriod.set(period);
    this.loadFinancial();
  }

  formatCurrency(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      minimumFractionDigits: 0
    }).format(value);
  }

  formatNumber(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return new Intl.NumberFormat('es-CO').format(value);
  }
}
