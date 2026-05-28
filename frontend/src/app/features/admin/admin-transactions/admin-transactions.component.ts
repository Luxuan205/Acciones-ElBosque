import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../core/services/admin.service';
import { AdminTransactionDto, AdminTransactionFilterDto } from '../../../core/models';

@Component({
  selector: 'app-admin-transactions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-transactions.component.html',
  styleUrl: './admin-transactions.component.scss'
})
export class AdminTransactionsComponent implements OnInit {
  private readonly adminSvc = inject(AdminService);

  transactions = signal<AdminTransactionDto[]>([]);
  totalElements = signal(0);
  totalPages = signal(0);
  currentPage = signal(0);
  readonly pageSize = 20;
  loading = signal(true);
  loadError = signal<string | null>(null);

  filterFrom       = signal('');
  filterTo         = signal('');
  filterInvestorId = signal('');
  filterSymbol     = signal('');
  filterType       = signal('');

  pageNumbers = computed(() =>
    Array.from({ length: Math.min(this.totalPages(), 7) }, (_, i) => i)
  );

  ngOnInit(): void {
    this.search();
  }

  private buildFilters(): AdminTransactionFilterDto {
    const f: AdminTransactionFilterDto = { page: this.currentPage(), size: this.pageSize };
    if (this.filterFrom())   f.from = this.filterFrom();
    if (this.filterTo())     f.to   = this.filterTo();
    const id = this.filterInvestorId().trim();
    if (id && !isNaN(Number(id))) f.investorId = Number(id);
    if (this.filterSymbol().trim()) f.symbol = this.filterSymbol().trim();
    if (this.filterType())   f.type = this.filterType();
    return f;
  }

  search(): void {
    this.currentPage.set(0);
    this.loadPage();
  }

  loadPage(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.adminSvc.getAdminTransactions(this.buildFilters()).subscribe({
      next: page => {
        this.transactions.set(page.content);
        this.totalElements.set(page.totalElements);
        this.totalPages.set(page.totalPages);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('Error al cargar las transacciones.');
        this.loading.set(false);
      }
    });
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadPage();
  }

  clearFilters(): void {
    this.filterFrom.set('');
    this.filterTo.set('');
    this.filterInvestorId.set('');
    this.filterSymbol.set('');
    this.filterType.set('');
    this.search();
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString('es-CO', { timeZone: 'America/Bogota' });
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency', currency: 'COP', minimumFractionDigits: 0
    }).format(value);
  }
}
