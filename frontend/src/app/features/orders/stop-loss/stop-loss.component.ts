import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { OrderService } from '../../../core/services/order.service';
import {
  OrderResponse,
  PlaceStopLossRequest
} from '../../../core/models';

@Component({
  selector: 'app-stop-loss',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './stop-loss.component.html',
  styleUrl: './stop-loss.component.scss'
})
export class StopLossComponent implements OnInit {
  private readonly orderSvc = inject(OrderService);
  private readonly fb = inject(FormBuilder);

  loading = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  result = signal<OrderResponse | null>(null);

  readonly typeOptions: { value: PlaceStopLossRequest['type']; label: string; description: string }[] = [
    { value: 'STOP_LOSS', label: 'Stop Loss', description: 'Vende cuando el precio cae al límite' },
    { value: 'TAKE_PROFIT', label: 'Take Profit', description: 'Vende cuando el precio sube al objetivo' },
    { value: 'BOTH', label: 'Ambos', description: 'Combina ambos' }
  ];

  form = this.fb.group({
    symbol: ['', [Validators.required, Validators.minLength(1)]],
    quantity: [1, [Validators.required, Validators.min(1)]],
    triggerPrice: [null as number | null, [Validators.required, Validators.min(0.01)]],
    targetPrice: [null as number | null],
    type: ['STOP_LOSS' as PlaceStopLossRequest['type'], Validators.required]
  });

  ngOnInit(): void {}

  onSubmit(): void {
    if (this.form.invalid) return;
    const { symbol, quantity, triggerPrice, targetPrice, type } = this.form.value;
    this.loading.set(true);
    this.error.set(null);
    this.success.set(null);
    this.result.set(null);

    const req: PlaceStopLossRequest = {
      symbol: symbol!,
      quantity: quantity!,
      triggerPrice: triggerPrice!,
      type: type!,
      ...(targetPrice ? { targetPrice } : {})
    };

    this.orderSvc.placeStopLoss(req).subscribe({
      next: data => {
        this.result.set(data);
        this.success.set(`Orden #${data.id} de tipo ${type} creada exitosamente.`);
        this.loading.set(false);
        this.form.reset({ symbol: '', quantity: 1, triggerPrice: null, targetPrice: null, type: 'STOP_LOSS' });
      },
      error: () => {
        this.error.set('Error al crear la orden. Intente nuevamente.');
        this.loading.set(false);
      }
    });
  }

  formatCOP(value: number): string {
    return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(value);
  }
}
