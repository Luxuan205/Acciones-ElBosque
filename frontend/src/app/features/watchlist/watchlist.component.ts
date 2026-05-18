import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { WatchlistService } from '../../core/services/watchlist.service';
import { MarketService } from '../../core/services/market.service';
import { WatchlistItem, StockDetail } from '../../core/models';

export interface WatchlistRow {
  item: WatchlistItem;
  detail: StockDetail | null;
  loadError: boolean;
}

@Component({
  selector: 'app-watchlist',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './watchlist.component.html',
  styleUrl: './watchlist.component.scss'
})
export class WatchlistComponent implements OnInit {
  private readonly watchlistService = inject(WatchlistService);
  private readonly marketService = inject(MarketService);

  rows = signal<WatchlistRow[]>([]);
  loading = signal(true);

  ngOnInit(): void {
    this.loadWatchlist();
  }

  loadWatchlist(): void {
    this.loading.set(true);
    const items = this.watchlistService.getWatchlist();

    if (items.length === 0) {
      this.rows.set([]);
      this.loading.set(false);
      return;
    }

    const requests = items.map(item =>
      this.marketService.getStock(item.symbol).pipe(
        catchError(() => of(null))
      )
    );

    forkJoin(requests).subscribe({
      next: details => {
        const mapped: WatchlistRow[] = items.map((item, i) => ({
          item,
          detail: details[i] as StockDetail | null,
          loadError: details[i] === null
        }));
        this.rows.set(mapped);
        this.loading.set(false);
      },
      error: () => {
        this.rows.set(items.map(item => ({ item, detail: null, loadError: true })));
        this.loading.set(false);
      }
    });
  }

  removeFromWatchlist(symbol: string): void {
    this.watchlistService.removeFromWatchlist(symbol);
    this.rows.update(current => current.filter(r => r.item.symbol !== symbol));
  }
}
