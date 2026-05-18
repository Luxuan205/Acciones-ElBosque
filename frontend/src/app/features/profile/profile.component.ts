import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { ProfileResponse } from '../../core/models';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss'],
})
export class ProfileComponent implements OnInit {
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);

  profile = signal<ProfileResponse | null>(null);
  loading = signal(true);
  editMode = signal(false);
  saving = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);

  form = this.fb.group({
    fullName: ['', [Validators.required, Validators.minLength(2)]],
    phone: [''],
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.authService.getProfile().subscribe({
      next: data => {
        this.profile.set(data);
        this.form.patchValue({ fullName: data.fullName, phone: data.phone ?? '' });
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el perfil.');
        this.loading.set(false);
      },
    });
  }

  toggleEdit(): void {
    if (this.editMode()) {
      const p = this.profile();
      if (p) this.form.patchValue({ fullName: p.fullName, phone: p.phone ?? '' });
    }
    this.editMode.update(v => !v);
    this.error.set(null);
    this.success.set(null);
  }

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    this.authService.updateProfile({ fullName: this.form.value.fullName!, phone: this.form.value.phone! }).subscribe({
      next: updated => {
        this.profile.set(updated);
        this.editMode.set(false);
        this.success.set('Perfil actualizado correctamente.');
        this.saving.set(false);
      },
      error: () => {
        this.error.set('Error al actualizar el perfil.');
        this.saving.set(false);
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
