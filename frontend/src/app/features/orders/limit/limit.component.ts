import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { OrderService } from '../../../core/services/order.service';
import { PortfolioService } from '../../../core/services/portfolio.service';
import {
  LimitOrderResponse,
  PlaceLimitBuyRequest,
  PlaceLimitSellRequest,
  PositionDto
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
  private readonly portfolioSvc = inject(PortfolioService);
  private readonly fb = inject(FormBuilder);

  loading = signal(false);
  loadingPositions = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  result = signal<LimitOrderResponse | null>(null);
  orderType = signal<'BUY' | 'SELL'>('BUY');

  positions = signal<PositionDto[]>([]);
  filteredPositions = signal<PositionDto[]>([]);
  selectedPosition = signal<PositionDto | null>(null);
  showDropdown = signal(false);
  positionSearch = '';

  form = this.fb.group({
    symbol: ['', [Validators.required, Validators.minLength(1)]],
    quantity: [1, [Validators.required, Validators.min(1)]],
    limitPrice: [null as number | null, [Validators.required, Validators.min(0.01)]],
    expiresAt: [null as string | null]
  });

  ngOnInit(): void {
    this.loadingPositions.set(true);
    this.portfolioSvc.getPositions().subscribe({
      next: response => {
        const positions = response.positions.filter(p => p.quantity > 0);
        this.positions.set(positions);
        this.filteredPositions.set(positions.slice(0, 12));
        this.loadingPositions.set(false);
      },
      error: () => this.loadingPositions.set(false)
    });
  }

  setOrderType(type: 'BUY' | 'SELL'): void {
    this.orderType.set(type);
    this.clearPosition();
    this.form.patchValue({ symbol: '' });
  }

  onPositionSearch(value: string): void {
    this.positionSearch = value;
    const q = value.toLowerCase();
    this.filteredPositions.set(
      this.positions()
        .filter(p => p.symbol.toLowerCase().includes(q) || p.name.toLowerCase().includes(q))
        .slice(0, 12)
    );
    this.showDropdown.set(true);
  }

  onPositionBlur(): void {
    setTimeout(() => this.showDropdown.set(false), 200);
  }

  selectPosition(pos: PositionDto): void {
    this.selectedPosition.set(pos);
    this.positionSearch = pos.symbol;
    this.form.patchValue({ symbol: pos.symbol });
    this.showDropdown.set(false);
  }

  clearPosition(): void {
    this.selectedPosition.set(null);
    this.positionSearch = '';
    this.filteredPositions.set(this.positions().slice(0, 12));
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
        this.clearPosition();
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
