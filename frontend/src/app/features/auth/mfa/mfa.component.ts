import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-mfa',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './mfa.component.html',
  styleUrl: './mfa.component.scss'
})
export class MfaComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  sessionToken = signal('');
  channel = signal('');
  maskedEmail = signal('');
  loading = signal(false);
  resending = signal(false);
  errorMsg = signal('');
  cooldown = signal(0);
  private cooldownTimer?: ReturnType<typeof setInterval>;

  form = this.fb.group({
    otpCode: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
  });

  get f() { return this.form.controls; }

  ngOnInit(): void {
    const params = this.route.snapshot.queryParams;
    this.sessionToken.set(params['sessionToken'] ?? '');
    this.channel.set(params['channel'] ?? 'EMAIL');
    this.maskedEmail.set(params['maskedEmail'] ?? '');

    if (!this.sessionToken()) {
      this.router.navigate(['/login']);
    }
  }

  ngOnDestroy(): void {
    if (this.cooldownTimer) clearInterval(this.cooldownTimer);
  }

  onSubmit(): void {
    if (this.form.invalid || this.loading()) return;

    this.errorMsg.set('');
    this.loading.set(true);

    this.auth.verifyMfa({
      sessionToken: this.sessionToken(),
      otpCode: this.f['otpCode'].value!
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: err => {
        this.loading.set(false);
        const msg = err?.error?.message || err?.error?.error || 'Código incorrecto o expirado. Intenta de nuevo.';
        this.errorMsg.set(msg);
        this.form.reset();
      }
    });
  }

  resendOtp(): void {
    if (this.cooldown() > 0 || this.resending()) return;

    this.errorMsg.set('');
    this.resending.set(true);

    this.auth.resendOtp(this.sessionToken()).subscribe({
      next: () => {
        this.resending.set(false);
        this.startCooldown(60);
      },
      error: err => {
        this.resending.set(false);
        const msg = err?.error?.message || 'No se pudo reenviar el código.';
        this.errorMsg.set(msg);
      }
    });
  }

  private startCooldown(seconds: number): void {
    this.cooldown.set(seconds);
    this.cooldownTimer = setInterval(() => {
      this.cooldown.update(v => {
        if (v <= 1) {
          clearInterval(this.cooldownTimer);
          return 0;
        }
        return v - 1;
      });
    }, 1000);
  }
}
