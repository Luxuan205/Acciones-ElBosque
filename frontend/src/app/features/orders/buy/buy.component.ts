import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { OrderService } from '../../../core/services/order.service';
import { MarketService } from '../../../core/services/market.service';
import {
  OrderPreviewResponse,
  OrderResponse,
  PlaceMarketBuyRequest,
  StockSummary
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
  private readonly marketSvc = inject(MarketService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);

  loading = signal(false);
  loadingPreview = signal(false);
  loadingStocks = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  orderWarning = signal<string | null>(null);
  marketNextOpen = signal<string | null>(null);
  preview = signal<OrderPreviewResponse | null>(null);
  result = signal<OrderResponse | null>(null);

  stocks = signal<StockSummary[]>([]);
  filteredStocks = signal<StockSummary[]>([]);
  selectedStock = signal<StockSummary | null>(null);
  showDropdown = signal(false);
  stockSearch = '';

  form = this.fb.group({
    symbol: ['', [Validators.required, Validators.minLength(1)]],
    quantity: [1, [Validators.required, Validators.min(1)]]
  });

  ngOnInit(): void {
    this.marketSvc.getMarketStatus().subscribe({
      next: s => this.marketNextOpen.set(s.nextOpen ?? null),
      error: () => {}
    });

    this.loadingStocks.set(true);
    this.marketSvc.getStocks().subscribe({
      next: stocks => {
        this.stocks.set(stocks);
        this.filteredStocks.set(stocks.slice(0, 12));
        this.loadingStocks.set(false);
        const symbol = this.route.snapshot.queryParams['symbol'];
        if (symbol) {
          const found = stocks.find(s => s.symbol === symbol);
          if (found) {
            this.selectStock(found);
          } else {
            this.form.patchValue({ symbol });
          }
        }
      },
      error: () => this.loadingStocks.set(false)
    });
  }

  onSearchInput(value: string): void {
    this.stockSearch = value;
    const q = value.toLowerCase();
    this.filteredStocks.set(
      this.stocks()
        .filter(s => s.symbol.toLowerCase().includes(q) || s.name.toLowerCase().includes(q))
        .slice(0, 12)
    );
    this.showDropdown.set(true);
  }

  onSearchBlur(): void {
    setTimeout(() => this.showDropdown.set(false), 200);
  }

  selectStock(stock: StockSummary): void {
    this.selectedStock.set(stock);
    this.stockSearch = stock.symbol;
    this.form.patchValue({ symbol: stock.symbol });
    this.showDropdown.set(false);
    this.preview.set(null);
    this.error.set(null);
  }

  clearStock(): void {
    this.selectedStock.set(null);
    this.stockSearch = '';
    this.form.patchValue({ symbol: '' });
    this.preview.set(null);
    this.filteredStocks.set(this.stocks().slice(0, 12));
  }

  onPreview(): void {
    if (this.form.invalid) return;
    const { symbol, quantity } = this.form.value;
    this.loadingPreview.set(true);
    this.error.set(null);
    this.orderWarning.set(null);
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
    const p = this.preview();
    const nextOpen = p?.nextOpen ?? this.marketNextOpen();
    const closedWarning = p && !p.marketOpen
      ? `El mercado está cerrado. La orden se ejecutará en la próxima apertura${nextOpen ? ' a las ' + nextOpen.slice(0, 5) : ''}.`
      : null;

    this.orderSvc.placeBuy(req).subscribe({
      next: data => {
        this.result.set(data);
        this.success.set(`Orden de compra creada exitosamente.`);
        this.orderWarning.set(closedWarning);
        this.loading.set(false);
        this.preview.set(null);
        this.selectedStock.set(null);
        this.stockSearch = '';
        this.form.reset({ symbol: '', quantity: 1 });
        this.filteredStocks.set(this.stocks().slice(0, 12));
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
