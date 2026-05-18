import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  loading = signal(false);
  errorMsg = signal('');
  showPassword = signal(false);

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  get f() { return this.form.controls; }

  onSubmit(): void {
    if (this.form.invalid || this.loading()) return;

    this.errorMsg.set('');
    this.loading.set(true);

    this.auth.login(this.form.value as any).subscribe({
      next: res => {
        this.loading.set(false);
        this.router.navigate(['/mfa'], {
          queryParams: {
            sessionToken: res.sessionToken,
            channel: res.channel,
            maskedEmail: res.maskedEmail ?? ''
          }
        });
      },
      error: err => {
        this.loading.set(false);
        const msg = err?.error?.message || err?.error?.error || 'Credenciales incorrectas. Verifica tu correo y contraseña.';
        this.errorMsg.set(msg);
      }
    });
  }

  togglePassword(): void { this.showPassword.update(v => !v); }
}
