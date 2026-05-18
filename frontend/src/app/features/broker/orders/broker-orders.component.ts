import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { BrokerService } from '../../../core/services/broker.service';
import { BrokerOrderResponse } from '../../../core/models';

@Component({
  selector: 'app-broker-orders',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './broker-orders.component.html',
  styleUrl: './broker-orders.component.scss'
})
export class BrokerOrdersComponent implements OnInit {
  private readonly brokerSvc = inject(BrokerService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);

  submitting = signal(false);
  submitError = signal<string | null>(null);
  submitSuccess = signal<string | null>(null);
  recentOrders = signal<BrokerOrderResponse[]>([]);

  orderTypes = ['MARKET_BUY', 'MARKET_SELL'];

  form = this.fb.group({
    clientId: [null as number | null, [Validators.required, Validators.min(1)]],
    symbol: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(10)]],
    quantity: [null as number | null, [Validators.required, Validators.min(1)]],
    orderType: ['MARKET_BUY', Validators.required]
  });

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['clientId']) {
        this.form.patchValue({ clientId: Number(params['clientId']) });
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.submitError.set(null);
    this.submitSuccess.set(null);

    const { clientId, symbol, quantity, orderType } = this.form.value;

    this.brokerSvc.placeOrderForClient({
      clientId: clientId!,
      symbol: symbol!.toUpperCase().trim(),
      quantity: quantity!,
      orderType: orderType!
    }).subscribe({
      next: response => {
        this.recentOrders.set([response, ...this.recentOrders()]);
        this.submitSuccess.set(`Orden #${response.orderId} colocada exitosamente con estado: ${response.status}`);
        this.form.reset({ orderType: 'MARKET_BUY' });
        this.submitting.set(false);
      },
      error: err => {
        this.submitError.set(err?.error?.message || 'Error al colocar la orden. Verifica los datos.');
        this.submitting.set(false);
      }
    });
  }

  isFieldInvalid(field: string): boolean {
    const control = this.form.get(field);
    return !!(control && control.invalid && control.touched);
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString('es-CO');
  }
}
