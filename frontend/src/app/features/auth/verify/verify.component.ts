import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-verify',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './verify.component.html',
  styleUrl: './verify.component.scss'
})
export class VerifyComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  status = signal<'loading' | 'success' | 'error'>('loading');
  message = signal('');

  ngOnInit(): void {
    const token = this.route.snapshot.queryParams['token'];

    if (!token) {
      this.status.set('error');
      this.message.set('Token de verificación no encontrado. Solicita un nuevo enlace.');
      return;
    }

    this.auth.verifyEmail(token).subscribe({
      next: res => {
        this.status.set('success');
        this.message.set(res.message || '¡Tu correo ha sido verificado exitosamente!');
      },
      error: err => {
        this.status.set('error');
        const msg = err?.error?.message || err?.error?.error || 'El enlace de verificación es inválido o ha expirado.';
        this.message.set(msg);
      }
    });
  }
}
