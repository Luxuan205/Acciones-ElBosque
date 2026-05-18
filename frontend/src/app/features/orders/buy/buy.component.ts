import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { OrderService } from '../../../core/services/order.service';
import {
  OrderPreviewResponse,
  OrderResponse,
  PlaceMarketBuyRequest
} from '../../../core/models';

@Component({
  selector: 'app-buy',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './buy.component.html',
  styleUrl: './buy.component.scss'
})
export class BuyComponent implements OnInit {
  private readonly orderSvc = inject(OrderService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);

  loading = signal(false);
  loadingPreview = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  preview = signal<OrderPreviewResponse | null>(null);
  result = signal<OrderResponse | null>(null);

  form = this.fb.group({
    symbol: ['', [Validators.required, Validators.minLength(1)]],
    quantity: [1, [Validators.required, Validators.min(1)]]
  });

  ngOnInit(): void {
    const symbol = this.route.snapshot.queryParams['symbol'];
    if (symbol) {
      this.form.patchValue({ symbol });
    }
  }

  onPreview(): void {
    if (this.form.invalid) return;
    const { symbol, quantity } = this.form.value;
    this.loadingPreview.set(true);
    this.error.set(null);
    this.preview.set(null);

    this.orderSvc.previewBuy(symbol!, quantity!).subscribe({
      next: data => {
        this.preview.set(data);
        this.loadingPreview.set(false);
      },
      error: () => {
        this.error.set('Error al obtener la vista previa. Verifique el símbolo e intente nuevamente.');
        this.loadingPreview.set(false);
      }
    });
  }

  onConfirm(): void {
    if (this.form.invalid) return;
    const { symbol, quantity } = this.form.value;
    this.loading.set(true);
    this.error.set(null);
    this.success.set(null);

    const req: PlaceMarketBuyRequest = { symbol: symbol!, quantity: quantity! };
    this.orderSvc.placeBuy(req).subscribe({
      next: data => {
        this.result.set(data);
        this.success.set(`Orden de compra #${data.id} creada exitosamente.`);
        this.loading.set(false);
        this.preview.set(null);
        this.form.reset({ symbol: '', quantity: 1 });
      },
      error: () => {
        this.error.set('Error al procesar la orden de compra. Intente nuevamente.');
        this.loading.set(false);
      }
    });
  }

  formatCOP(value: number): string {
    return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(value);
  }
}
