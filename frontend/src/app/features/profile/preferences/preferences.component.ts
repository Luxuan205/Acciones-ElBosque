import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-preferences',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './preferences.component.html',
  styleUrls: ['./preferences.component.scss'],
})
export class PreferencesComponent implements OnInit {
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);

  loading = signal(true);
  saving = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);

  form = this.fb.group({
    emailNotifications: [true],
    language: ['es'],
    timezone: ['America/Bogota'],
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.authService.getPreferences().subscribe({
      next: prefs => {
        this.form.patchValue({
          emailNotifications: prefs['emailNotifications'] !== undefined
            ? Boolean(prefs['emailNotifications'])
            : true,
          language: (prefs['language'] as string) ?? 'es',
          timezone: (prefs['timezone'] as string) ?? 'America/Bogota',
        });
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  save(): void {
    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    this.authService.updatePreferences(this.form.value as Record<string, unknown>).subscribe({
      next: () => {
        this.success.set('Preferencias guardadas correctamente.');
        this.saving.set(false);
      },
      error: () => {
        this.error.set('Error al guardar las preferencias.');
        this.saving.set(false);
      },
    });
  }
}
