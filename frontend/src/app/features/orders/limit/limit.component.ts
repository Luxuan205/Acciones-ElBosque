import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { OrderService } from '../../../core/services/order.service';
import {
  LimitOrderResponse,
  PlaceLimitBuyRequest,
  PlaceLimitSellRequest
} from '../../../core/models';

@Component({
  selector: 'app-limit',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './limit.component.html',
  styleUrl: './limit.component.scss'
})
export class LimitComponent implements OnInit {
  private readonly orderSvc = inject(OrderService);
  private readonly fb = inject(FormBuilder);

  loading = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  result = signal<LimitOrderResponse | null>(null);
  orderType = signal<'BUY' | 'SELL'>('BUY');

  form = this.fb.group({
    symbol: ['', [Validators.required, Validators.minLength(1)]],
    quantity: [1, [Validators.required, Validators.min(1)]],
    limitPrice: [null as number | null, [Validators.required, Validators.min(0.01)]],
    expiresAt: [null as string | null]
  });

  ngOnInit(): void {}

  setOrderType(type: 'BUY' | 'SELL'): void {
    this.orderType.set(type);
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    const { symbol, quantity, limitPrice, expiresAt } = this.form.value;
    this.loading.set(true);
    this.error.set(null);
    this.success.set(null);
    this.result.set(null);

    const baseReq = {
      symbol: symbol!,
      quantity: quantity!,
      limitPrice: limitPrice!,
      ...(expiresAt ? { expiresAt } : {})
    };

    const obs$ = this.orderType() === 'BUY'
      ? this.orderSvc.placeLimitBuy(baseReq as PlaceLimitBuyRequest)
      : this.orderSvc.placeLimitSell(baseReq as PlaceLimitSellRequest);

    obs$.subscribe({
      next: data => {
        this.result.set(data);
        this.success.set(`Orden límite #${data.id} (${data.orderType}) creada exitosamente.`);
        this.loading.set(false);
        this.form.reset({ symbol: '', quantity: 1, limitPrice: null, expiresAt: null });
      },
      error: () => {
        this.error.set('Error al crear la orden límite. Intente nuevamente.');
        this.loading.set(false);
      }
    });
  }

  formatCOP(value: number): string {
    return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(value);
  }
}
