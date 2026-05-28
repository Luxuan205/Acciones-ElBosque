import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';
import { AuditEventDto, AdminUserDetailDto } from '../../../core/models';

@Component({
  selector: 'app-admin-audit-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-audit-detail.component.html',
  styleUrl: './admin-audit-detail.component.scss'
})
export class AdminAuditDetailComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly adminSvc = inject(AdminService);

  event = signal<AuditEventDto | null>(null);
  investor = signal<AdminUserDetailDto | null>(null);
  performedByUser = signal<AdminUserDetailDto | null>(null);
  loadingInvestor = signal(false);

  ngOnInit(): void {
    const state = this.router.lastSuccessfulNavigation?.extras?.state as { event?: AuditEventDto } | undefined;
    const ev = state?.['event'];
    if (!ev) { this.router.navigate(['/admin/audit']); return; }
    this.event.set(ev);

    if (ev.investorId) {
      this.loadingInvestor.set(true);
      this.adminSvc.getUserDetail(ev.investorId).subscribe({
        next: u => { this.investor.set(u); this.loadingInvestor.set(false); },
        error: () => this.loadingInvestor.set(false)
      });
    }

    if (ev.performedBy && ev.performedBy !== ev.investorId) {
      this.adminSvc.getUserDetail(ev.performedBy).subscribe({
        next: u => this.performedByUser.set(u),
        error: () => {}
      });
    }
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString('es-CO', {
      timeZone: 'America/Bogota',
      year: 'numeric', month: 'long', day: 'numeric',
      hour: '2-digit', minute: '2-digit', second: '2-digit'
    });
  }
}
