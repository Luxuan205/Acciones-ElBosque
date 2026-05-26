import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { OrderService } from '../../../core/services/order.service';
import { PortfolioService } from '../../../core/services/portfolio.service';
import {
  SellOrderPreviewResponse,
  OrderResponse,
  PlaceMarketSellRequest,
  PositionDto
} from '../../../core/models';

@Component({
  selector: 'app-sell',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './sell.component.html',
  styleUrl: './sell.component.scss'
})
export class SellComponent implements OnInit {
  private readonly orderSvc = inject(OrderService);
  private readonly portfolioSvc = inject(PortfolioService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);

  loading = signal(false);
  loadingPreview = signal(false);
  loadingStocks = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  preview = signal<SellOrderPreviewResponse | null>(null);
  result = signal<OrderResponse | null>(null);

  positions = signal<PositionDto[]>([]);
  filteredPositions = signal<PositionDto[]>([]);
  selectedPosition = signal<PositionDto | null>(null);
  showDropdown = signal(false);
  positionSearch = '';

  form = this.fb.group({
    symbol: ['', [Validators.required, Validators.minLength(1)]],
    quantity: [1, [Validators.required, Validators.min(1)]]
  });

  ngOnInit(): void {
    this.loadingStocks.set(true);
    this.portfolioSvc.getPositions().subscribe({
      next: response => {
        const positions = response.positions.filter(p => p.quantity > 0);
        this.positions.set(positions);
        this.filteredPositions.set(positions.slice(0, 12));
        this.loadingStocks.set(false);
        const symbol = this.route.snapshot.queryParams['symbol'];
        if (symbol) {
          const found = positions.find(p => p.symbol === symbol);
          if (found) this.selectPosition(found);
        }
      },
      error: () => this.loadingStocks.set(false)
    });
  }

  onSearchInput(value: string): void {
    this.positionSearch = value;
    const q = value.toLowerCase();
    this.filteredPositions.set(
      this.positions()
        .filter(p => p.symbol.toLowerCase().includes(q) || p.name.toLowerCase().includes(q))
        .slice(0, 12)
    );
    this.showDropdown.set(true);
  }

  onSearchBlur(): void {
    setTimeout(() => this.showDropdown.set(false), 200);
  }

  selectPosition(pos: PositionDto): void {
    this.selectedPosition.set(pos);
    this.positionSearch = pos.symbol;
    this.form.patchValue({ symbol: pos.symbol });
    this.showDropdown.set(false);
    this.preview.set(null);
    this.error.set(null);
  }

  clearPosition(): void {
    this.selectedPosition.set(null);
    this.positionSearch = '';
    this.form.patchValue({ symbol: '' });
    this.preview.set(null);
    this.filteredPositions.set(this.positions().slice(0, 12));
  }

  onPreview(): void {
    if (this.form.invalid) return;
    const { symbol, quantity } = this.form.value;
    this.loadingPreview.set(true);
    this.error.set(null);
    this.preview.set(null);

    this.orderSvc.previewSell(symbol!, quantity!).subscribe({
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

    const req: PlaceMarketSellRequest = { symbol: symbol!, quantity: quantity! };
    this.orderSvc.placeSell(req).subscribe({
      next: data => {
        this.result.set(data);
        this.success.set(`Orden de venta #${data.id} creada exitosamente.`);
        this.loading.set(false);
        this.preview.set(null);
        this.selectedPosition.set(null);
        this.positionSearch = '';
        this.form.reset({ symbol: '', quantity: 1 });
        this.filteredPositions.set(this.positions().slice(0, 12));
      },
      error: () => {
        this.error.set('Error al procesar la orden de venta. Intente nuevamente.');
        this.loading.set(false);
      }
    });
  }

  formatCOP(value: number): string {
    return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(value);
  }
}
