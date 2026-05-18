import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PortfolioService } from '../../../core/services/portfolio.service';
import {
  BalanceSummaryResponse,
  FundMovementDto,
  FundMovementPageResponse
} from '../../../core/models';

const POSITIVE_TYPES = new Set(['DEPOSIT', 'DIVIDEND', 'SELL']);

@Component({
  selector: 'app-balance',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './balance.component.html',
  styleUrl: './balance.component.scss'
})
export class BalanceComponent implements OnInit {
  private readonly portfolioService = inject(PortfolioService);

  balance = signal<BalanceSummaryResponse | null>(null);
  movements = signal<FundMovementDto[]>([]);
  totalElements = signal(0);
  currentPage = signal(0);
  pageSize = 10;

  loadingBalance = signal(true);
  loadingMovements = signal(true);
  errorBalance = signal<string | null>(null);
  errorMovements = signal<string | null>(null);

  private readonly cop = new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'COP',
    minimumFractionDigits: 0
  });

  ngOnInit(): void {
    this.loadBalance();
    this.loadMovements(0);
  }

  loadBalance(): void {
    this.loadingBalance.set(true);
    this.errorBalance.set(null);
    this.portfolioService.getBalance().subscribe({
      next: data => {
        this.balance.set(data);
        this.loadingBalance.set(false);
      },
      error: () => {
        this.errorBalance.set('Error al cargar el balance. Intenta de nuevo.');
        this.loadingBalance.set(false);
      }
    });
  }

  loadMovements(page: number): void {
    this.loadingMovements.set(true);
    this.errorMovements.set(null);
    this.portfolioService.getMovements(page, this.pageSize).subscribe({
      next: (data: FundMovementPageResponse) => {
        this.movements.set(data.content);
        this.totalElements.set(data.totalElements);
        this.currentPage.set(page);
        this.loadingMovements.set(false);
      },
      error: () => {
        this.errorMovements.set('Error al cargar los movimientos.');
        this.loadingMovements.set(false);
      }
    });
  }

  prevPage(): void {
    if (this.currentPage() > 0) {
      this.loadMovements(this.currentPage() - 1);
    }
  }

  nextPage(): void {
    const totalPages = Math.ceil(this.totalElements() / this.pageSize);
    if (this.currentPage() < totalPages - 1) {
      this.loadMovements(this.currentPage() + 1);
    }
  }

  get hasPrev(): boolean {
    return this.currentPage() > 0;
  }

  get hasNext(): boolean {
    const totalPages = Math.ceil(this.totalElements() / this.pageSize);
    return this.currentPage() < totalPages - 1;
  }

  formatCOP(value: number): string {
    return this.cop.format(value);
  }

  isPositiveMovement(movementType: string): boolean {
    return POSITIVE_TYPES.has(movementType);
  }
}
