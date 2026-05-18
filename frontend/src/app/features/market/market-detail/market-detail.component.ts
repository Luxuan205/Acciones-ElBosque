import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { MarketService } from '../../../core/services/market.service';
import { WatchlistService } from '../../../core/services/watchlist.service';
import { StockDetail, IntradayData, IntradayPoint } from '../../../core/models';

@Component({
  selector: 'app-market-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './market-detail.component.html',
  styleUrl: './market-detail.component.scss'
})
export class MarketDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly marketService = inject(MarketService);
  private readonly watchlistService = inject(WatchlistService);

  symbol = signal<string>('');
  stock = signal<StockDetail | null>(null);
  intraday = signal<IntradayData | null>(null);
  loading = signal(true);
  loadingChart = signal(true);
  error = signal<string | null>(null);

  readonly svgWidth = 600;
  readonly svgHeight = 120;

  ngOnInit(): void {
    const sym = this.route.snapshot.paramMap.get('symbol') ?? '';
    this.symbol.set(sym);

    this.marketService.getStock(sym).subscribe({
      next: data => {
        this.stock.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar los datos de la acción.');
        this.loading.set(false);
      }
    });

    this.marketService.getIntraday(sym).subscribe({
      next: data => {
        this.intraday.set(data);
        this.loadingChart.set(false);
      },
      error: () => {
        this.loadingChart.set(false);
      }
    });
  }

  isWatched(): boolean {
    return this.watchlistService.isWatched(this.symbol());
  }

  toggleWatchlist(): void {
    const s = this.stock();
    if (!s) return;
    if (this.watchlistService.isWatched(s.symbol)) {
      this.watchlistService.removeFromWatchlist(s.symbol);
    } else {
      this.watchlistService.addToWatchlist(s.symbol, s.name);
    }
  }

  buildPolylinePoints(points: IntradayPoint[]): string {
    if (!points || points.length < 2) return '';

    const prices = points.map(p => p.price);
    const minPrice = Math.min(...prices);
    const maxPrice = Math.max(...prices);
    const priceRange = maxPrice - minPrice || 1;
    const padding = 8;

    return points
      .map((point, index) => {
        const x = (index / (points.length - 1)) * this.svgWidth;
        const y =
          this.svgHeight -
          padding -
          ((point.price - minPrice) / priceRange) * (this.svgHeight - padding * 2);
        return `${x.toFixed(1)},${y.toFixed(1)}`;
      })
      .join(' ');
  }
}
