import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PortfolioService } from '../../../core/services/portfolio.service';
import { PortfolioPositionsResponse, PositionDto } from '../../../core/models';

@Component({
  selector: 'app-positions',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './positions.component.html',
  styleUrl: './positions.component.scss'
})
export class PositionsComponent implements OnInit {
  private readonly portfolioService = inject(PortfolioService);

  positionsData = signal<PortfolioPositionsResponse | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);

  private readonly cop = new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'COP',
    minimumFractionDigits: 0
  });

  ngOnInit(): void {
    this.loadPositions();
  }

  loadPositions(): void {
    this.loading.set(true);
    this.error.set(null);
    this.portfolioService.getPositions().subscribe({
      next: data => {
        this.positionsData.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar las posiciones. Intenta de nuevo.');
        this.loading.set(false);
      }
    });
  }

  formatCOP(value: number): string {
    return this.cop.format(value);
  }

  formatPercent(value: number): string {
    const sign = value >= 0 ? '+' : '';
    return `${sign}${value.toFixed(2)}%`;
  }

  formatGain(value: number): string {
    const sign = value >= 0 ? '+' : '';
    return `${sign}${this.cop.format(value)}`;
  }

  trackBySymbol(_index: number, position: PositionDto): string {
    return position.symbol;
  }
}
