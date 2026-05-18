import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../core/services/admin.service';
import {
  GlobalParameterDto,
  ParameterChangeHistoryDto
} from '../../../core/models';

interface EditState {
  key: string;
  value: string;
  reason: string;
}

@Component({
  selector: 'app-admin-parameters',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-parameters.component.html',
  styleUrl: './admin-parameters.component.scss'
})
export class AdminParametersComponent implements OnInit {
  private readonly adminSvc = inject(AdminService);

  groupedParams = signal<Record<string, GlobalParameterDto[]>>({});
  categories = signal<string[]>([]);
  loading = signal(true);
  loadError = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  // Inline editing
  editingKey = signal<string | null>(null);
  editState = signal<EditState>({ key: '', value: '', reason: '' });
  saveError = signal<string | null>(null);
  saving = signal(false);

  // History modal
  showHistoryModal = signal(false);
  historyKey = signal<string | null>(null);
  history = signal<ParameterChangeHistoryDto[]>([]);
  loadingHistory = signal(false);

  ngOnInit(): void {
    this.loadParameters();
  }

  loadParameters(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.adminSvc.getParameters().subscribe({
      next: resp => {
        this.groupedParams.set(resp.parameters);
        this.categories.set(Object.keys(resp.parameters).sort());
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('Error al cargar los parámetros del sistema.');
        this.loading.set(false);
      }
    });
  }

  startEdit(param: GlobalParameterDto): void {
    this.editingKey.set(param.key);
    this.editState.set({ key: param.key, value: param.value, reason: '' });
    this.saveError.set(null);
  }

  cancelEdit(): void {
    this.editingKey.set(null);
    this.saveError.set(null);
  }

  saveEdit(): void {
    const state = this.editState();
    if (!state.value.trim()) {
      this.saveError.set('El valor no puede estar vacío.');
      return;
    }
    this.saving.set(true);
    this.saveError.set(null);
    this.adminSvc.updateParameter(state.key, { value: state.value.trim(), reason: state.reason || undefined }).subscribe({
      next: resp => {
        const grouped = { ...this.groupedParams() };
        for (const cat of this.categories()) {
          grouped[cat] = grouped[cat].map(p =>
            p.key === state.key ? { ...p, value: state.value.trim() } : p
          );
        }
        this.groupedParams.set(grouped);
        this.successMessage.set(resp.message || `Parámetro ${state.key} actualizado.`);
        this.editingKey.set(null);
        this.saving.set(false);
        setTimeout(() => this.successMessage.set(null), 4000);
      },
      error: err => {
        this.saveError.set(err?.error?.message || 'Error al guardar el parámetro.');
        this.saving.set(false);
      }
    });
  }

  revertParam(key: string): void {
    if (!confirm(`¿Revertir el parámetro "${key}" a su valor anterior?`)) return;
    this.adminSvc.revertParameter(key).subscribe({
      next: resp => {
        this.successMessage.set(resp.message || `Parámetro ${key} revertido.`);
        this.loadParameters();
        setTimeout(() => this.successMessage.set(null), 4000);
      },
      error: err => alert(err?.error?.message || 'Error al revertir el parámetro.')
    });
  }

  openHistory(key: string): void {
    this.historyKey.set(key);
    this.showHistoryModal.set(true);
    this.loadingHistory.set(true);
    this.adminSvc.getParameterHistory(key).subscribe({
      next: data => { this.history.set(data); this.loadingHistory.set(false); },
      error: () => this.loadingHistory.set(false)
    });
  }

  closeHistory(): void {
    this.showHistoryModal.set(false);
    this.history.set([]);
  }

  updateEditValue(value: string): void {
    this.editState.update(s => ({ ...s, value }));
  }

  updateEditReason(reason: string): void {
    this.editState.update(s => ({ ...s, reason }));
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString('es-CO');
  }
}
