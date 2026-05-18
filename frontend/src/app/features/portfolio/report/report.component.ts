import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PortfolioService } from '../../../core/services/portfolio.service';
import { PortfolioReportDto, TransactionDto } from '../../../core/models';

export type ReportPeriod = 'TODAY' | 'WEEK' | 'MONTH';

@Component({
  selector: 'app-report',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './report.component.html',
  styleUrl: './report.component.scss'
})
export class ReportComponent implements OnInit {
  private readonly portfolioService = inject(PortfolioService);

  report = signal<PortfolioReportDto | null>(null);
  selectedPeriod = signal<ReportPeriod>('MONTH');
  loading = signal(true);
  error = signal<string | null>(null);
  exporting = signal(false);

  readonly periods: ReportPeriod[] = ['TODAY', 'WEEK', 'MONTH'];
  readonly periodLabels: Record<ReportPeriod, string> = {
    TODAY: 'Hoy',
    WEEK: 'Semana',
    MONTH: 'Mes'
  };

  private readonly cop = new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'COP',
    minimumFractionDigits: 0
  });

  ngOnInit(): void {
    this.loadReport(this.selectedPeriod());
  }

  selectPeriod(period: ReportPeriod): void {
    this.selectedPeriod.set(period);
    this.loadReport(period);
  }

  loadReport(period: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.portfolioService.getReport(period).subscribe({
      next: data => {
        this.report.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar el reporte. Intenta de nuevo.');
        this.loading.set(false);
      }
    });
  }

  exportReport(): void {
    this.exporting.set(true);
    this.portfolioService.exportReport(this.selectedPeriod()).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `reporte-${this.selectedPeriod().toLowerCase()}.csv`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting.set(false);
      },
      error: () => {
        this.error.set('Error al exportar el reporte.');
        this.exporting.set(false);
      }
    });
  }

  formatCOP(value: number): string {
    return this.cop.format(value);
  }

  formatGain(value: number): string {
    if (value === undefined || value === null) return '—';
    const sign = value >= 0 ? '+' : '';
    return `${sign}${this.cop.format(value)}`;
  }

  trackByTx(_index: number, tx: TransactionDto): number {
    return tx.id;
  }
}
