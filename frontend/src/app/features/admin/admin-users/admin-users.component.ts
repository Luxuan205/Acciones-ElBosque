import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { AdminService } from '../../../core/services/admin.service';
import {
  AdminUserDto,
  AdminUserDetailDto,
  UpdateUserStatusRequest,
  UpdateUserRoleRequest
} from '../../../core/models';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.scss'
})
export class AdminUsersComponent implements OnInit {
  private readonly adminSvc = inject(AdminService);

  // List state
  users = signal<AdminUserDto[]>([]);
  totalElements = signal(0);
  currentPage = signal(0);
  pageSize = 15;
  loading = signal(true);
  listError = signal<string | null>(null);

  // Filters
  searchQuery = signal('');
  statusFilter = signal('');
  roleFilter = signal('');
  private searchDebounce: ReturnType<typeof setTimeout> | null = null;

  // Detail modal
  showDetailModal = signal(false);
  selectedUserDetail = signal<AdminUserDetailDto | null>(null);
  loadingDetail = signal(false);

  // Status modal
  showStatusModal = signal(false);
  selectedUser = signal<AdminUserDto | null>(null);
  newStatus = signal('');
  statusReason = signal('');
  statusSubmitting = signal(false);
  statusError = signal<string | null>(null);

  // Role modal
  showRoleModal = signal(false);
  newRole = signal('');
  roleReason = signal('');
  roleConfirmed = signal(false);
  roleSubmitting = signal(false);
  roleError = signal<string | null>(null);

  // Success message
  successMessage = signal<string | null>(null);

  statusOptions = ['ACTIVE', 'SUSPENDED', 'PENDING', 'BLOCKED'];
  roleOptions = ['INVESTOR', 'BROKER', 'ADMIN'];

  totalPages = computed(() => Math.ceil(this.totalElements() / this.pageSize));
  pageNumbers = computed(() => {
    const total = this.totalPages();
    return Array.from({ length: Math.min(total, 7) }, (_, i) => i);
  });

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.listError.set(null);
    this.adminSvc.getUsers({
      search: this.searchQuery() || undefined,
      status: this.statusFilter() || undefined,
      role: this.roleFilter() || undefined,
      page: this.currentPage(),
      size: this.pageSize
    }).subscribe({
      next: resp => {
        this.users.set(resp.content);
        this.totalElements.set(resp.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.listError.set('Error al cargar usuarios.');
        this.loading.set(false);
      }
    });
  }

  onSearchInput(value: string): void {
    this.searchQuery.set(value);
    if (this.searchDebounce) clearTimeout(this.searchDebounce);
    this.searchDebounce = setTimeout(() => {
      this.currentPage.set(0);
      this.loadUsers();
    }, 400);
  }

  onFilterChange(): void {
    this.currentPage.set(0);
    this.loadUsers();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadUsers();
  }

  // Detail
  openDetailModal(user: AdminUserDto): void {
    this.selectedUser.set(user);
    this.showDetailModal.set(true);
    this.loadingDetail.set(true);
    this.adminSvc.getUserDetail(user.id).subscribe({
      next: detail => { this.selectedUserDetail.set(detail); this.loadingDetail.set(false); },
      error: () => this.loadingDetail.set(false)
    });
  }

  closeDetailModal(): void {
    this.showDetailModal.set(false);
    this.selectedUserDetail.set(null);
  }

  // Status modal
  openStatusModal(user: AdminUserDto): void {
    this.selectedUser.set(user);
    this.newStatus.set(user.accountStatus);
    this.statusReason.set('');
    this.statusError.set(null);
    this.showStatusModal.set(true);
  }

  closeStatusModal(): void {
    this.showStatusModal.set(false);
  }

  submitStatusChange(): void {
    if (!this.newStatus() || !this.statusReason().trim()) {
      this.statusError.set('El nuevo estado y la razón son requeridos.');
      return;
    }
    this.statusSubmitting.set(true);
    this.statusError.set(null);
    const req: UpdateUserStatusRequest = { newStatus: this.newStatus(), reason: this.statusReason() };
    this.adminSvc.updateUserStatus(this.selectedUser()!.id, req).subscribe({
      next: updated => {
        this.users.set(this.users().map(u => u.id === updated.id ? updated : u));
        this.successMessage.set(`Estado del usuario ${updated.fullName} actualizado a ${updated.accountStatus}.`);
        this.statusSubmitting.set(false);
        this.showStatusModal.set(false);
        setTimeout(() => this.successMessage.set(null), 4000);
      },
      error: err => {
        this.statusError.set(err?.error?.message || 'Error al cambiar el estado.');
        this.statusSubmitting.set(false);
      }
    });
  }

  // Role modal
  openRoleModal(user: AdminUserDto): void {
    this.selectedUser.set(user);
    this.newRole.set(user.role);
    this.roleReason.set('');
    this.roleConfirmed.set(false);
    this.roleError.set(null);
    this.showRoleModal.set(true);
  }

  closeRoleModal(): void {
    this.showRoleModal.set(false);
  }

  submitRoleChange(): void {
    if (!this.newRole() || !this.roleReason().trim()) {
      this.roleError.set('El nuevo rol y la razón son requeridos.');
      return;
    }
    if (!this.roleConfirmed()) {
      this.roleError.set('Debes confirmar que entiendes las implicaciones del cambio de rol.');
      return;
    }
    this.roleSubmitting.set(true);
    this.roleError.set(null);
    const req: UpdateUserRoleRequest = { newRole: this.newRole(), reason: this.roleReason(), confirmed: true };
    this.adminSvc.updateUserRole(this.selectedUser()!.id, req).subscribe({
      next: updated => {
        this.users.set(this.users().map(u => u.id === updated.id ? updated : u));
        this.successMessage.set(`Rol del usuario ${updated.fullName} cambiado a ${updated.role}.`);
        this.roleSubmitting.set(false);
        this.showRoleModal.set(false);
        setTimeout(() => this.successMessage.set(null), 4000);
      },
      error: err => {
        this.roleError.set(err?.error?.message || 'Error al cambiar el rol.');
        this.roleSubmitting.set(false);
      }
    });
  }

  // Reset password
  resetPassword(user: AdminUserDto): void {
    if (!confirm(`¿Resetear la contraseña de ${user.fullName}? Se enviará un correo al usuario.`)) return;
    this.adminSvc.resetPassword(user.id).subscribe({
      next: resp => {
        this.successMessage.set(resp.message || `Contraseña reseteada para ${user.fullName}.`);
        setTimeout(() => this.successMessage.set(null), 4000);
      },
      error: () => alert('Error al resetear la contraseña.')
    });
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('es-CO');
  }
}
