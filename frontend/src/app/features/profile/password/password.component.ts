import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AbstractControl, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';

function passwordMatchValidator(g: AbstractControl) {
  const pw = g.get('newPassword')?.value;
  const confirm = g.get('confirmPassword')?.value;
  return pw === confirm ? null : { mismatch: true };
}

@Component({
  selector: 'app-password',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './password.component.html',
  styleUrls: ['./password.component.scss'],
})
export class PasswordComponent {
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);

  submitting = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);

  form = this.fb.group(
    {
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required],
    },
    { validators: passwordMatchValidator }
  );

  get newPassword() {
    return this.form.get('newPassword');
  }

  get confirmPassword() {
    return this.form.get('confirmPassword');
  }

  submit(): void {
    if (this.form.invalid) return;
    this.submitting.set(true);
    this.error.set(null);
    this.success.set(null);
    const { currentPassword, newPassword, confirmPassword } = this.form.value;
    this.authService
      .changePassword({
        currentPassword: currentPassword!,
        newPassword: newPassword!,
        confirmPassword: confirmPassword!,
      })
      .subscribe({
        next: () => {
          this.success.set('Contraseña actualizada correctamente.');
          this.form.reset();
          this.submitting.set(false);
        },
        error: (err) => {
          this.error.set(err?.error?.message ?? 'Error al cambiar la contraseña.');
          this.submitting.set(false);
        },
      });
  }
}
