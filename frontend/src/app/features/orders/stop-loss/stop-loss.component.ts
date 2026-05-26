import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { OrderService } from '../../../core/services/order.service';
import { PortfolioService } from '../../../core/services/portfolio.service';
import {
  OrderResponse,
  PlaceStopLossRequest,
  PositionDto
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
  private readonly portfolioSvc = inject(PortfolioService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  loading = signal(false);
  loadingPositions = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  result = signal<OrderResponse | null>(null);

  positions = signal<PositionDto[]>([]);
  filteredPositions = signal<PositionDto[]>([]);
  selectedPosition = signal<PositionDto | null>(null);
  showDropdown = signal(false);
  positionSearch = '';

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
    this.form.patchValue({ symbol: '' });
    this.filteredPositions.set(this.positions().slice(0, 12));
  }

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
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
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
