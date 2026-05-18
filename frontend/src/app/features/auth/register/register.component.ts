import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const parent = control.parent;
  if (!parent) return null;
  const pass = parent.get('password')?.value;
  const confirm = control.value;
  return pass === confirm ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  loading = signal(false);
  errorMsg = signal('');
  successMsg = signal('');
  showPassword = signal(false);
  showConfirm = signal(false);

  form = this.fb.group({
    fullName: ['', [Validators.required, Validators.minLength(3)]],
    documentNumber: ['', [Validators.required, Validators.pattern(/^\d{6,12}$/)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required, passwordMatchValidator]]
  });

  get f() { return this.form.controls; }

  onSubmit(): void {
    if (this.form.invalid || this.loading()) return;

    this.errorMsg.set('');
    this.loading.set(true);

    this.auth.register(this.form.value as any).subscribe({
      next: res => {
        this.loading.set(false);
        this.successMsg.set(res.message || 'Registro exitoso. Revisa tu correo para verificar tu cuenta.');
        this.form.reset();
      },
      error: err => {
        this.loading.set(false);
        const msg = err?.error?.message || err?.error?.error || 'Error al registrarse. Intenta de nuevo.';
        this.errorMsg.set(msg);
      }
    });
  }

  togglePassword(): void { this.showPassword.update(v => !v); }
  toggleConfirm(): void  { this.showConfirm.update(v => !v); }
}
