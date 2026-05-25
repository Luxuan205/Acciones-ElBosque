import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../core/services/admin.service';
import { AuditEventDto, AuditFilterDto } from '../../../core/models';

@Component({
  selector: 'app-admin-audit',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-audit.component.html',
  styleUrl: './admin-audit.component.scss'
})
export class AdminAuditComponent implements OnInit {
  private readonly adminSvc = inject(AdminService);

  events = signal<AuditEventDto[]>([]);
  totalElements = signal(0);
  totalPages = signal(0);
  currentPage = signal(0);
  pageSize = 20;
  loading = signal(true);
  loadError = signal<string | null>(null);

  // Filters
  filterEventType = signal('');
  filterInvestorId = signal<string>('');
  filterResult = signal('');
  filterFrom = signal('');
  filterTo = signal('');

  resultOptions = ['', 'SUCCESS', 'FAILURE', 'ERROR'];

  pageNumbers = computed(() =>
    Array.from({ length: Math.min(this.totalPages(), 7) }, (_, i) => i)
  );

  ngOnInit(): void {
    this.search();
  }

  buildFilters(): AuditFilterDto {
    const filters: AuditFilterDto = {
      page: this.currentPage(),
      size: this.pageSize
    };
    if (this.filterEventType().trim()) filters.eventType = this.filterEventType().trim();
    const investorIdStr = this.filterInvestorId().trim();
    if (investorIdStr && !isNaN(Number(investorIdStr))) filters.investorId = Number(investorIdStr);
    if (this.filterResult()) filters.result = this.filterResult();
    if (this.filterFrom()) filters.from = this.filterFrom();
    if (this.filterTo()) filters.to = this.filterTo();
    return filters;
  }

  search(): void {
    this.currentPage.set(0);
    this.loadPage();
  }

  loadPage(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.adminSvc.getAuditEvents(this.buildFilters()).subscribe({
      next: page => {
        this.events.set(page.content);
        this.totalElements.set(page.totalElements);
        this.totalPages.set(page.totalPages);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('Error al cargar los eventos de auditoría.');
        this.loading.set(false);
      }
    });
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadPage();
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString('es-CO', { timeZone: 'America/Bogota' });
  }

  clearFilters(): void {
    this.filterEventType.set('');
    this.filterInvestorId.set('');
    this.filterResult.set('');
    this.filterFrom.set('');
    this.filterTo.set('');
    this.search();
  }
}
