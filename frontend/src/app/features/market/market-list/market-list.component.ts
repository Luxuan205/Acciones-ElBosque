import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { MarketService } from '../../../core/services/market.service';
import { WatchlistService } from '../../../core/services/watchlist.service';
import { StockSummary } from '../../../core/models';

@Component({
  selector: 'app-market-list',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './market-list.component.html',
  styleUrl: './market-list.component.scss'
})
export class MarketListComponent implements OnInit {
  private readonly marketService = inject(MarketService);
  private readonly watchlistService = inject(WatchlistService);

  stocks = signal<StockSummary[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  searchControl = new FormControl('');
  sortControl = new FormControl('name_asc');

  ngOnInit(): void {
    this.loadStocks();

    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(() => this.loadStocks());

    this.sortControl.valueChanges.subscribe(() => this.loadStocks());
  }

  loadStocks(): void {
    this.loading.set(true);
    this.error.set(null);
    const search = this.searchControl.value ?? undefined;
    const sort = this.sortControl.value ?? undefined;

    this.marketService.getStocks(search || undefined, sort || undefined).subscribe({
      next: data => {
        this.stocks.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar los datos del mercado. Intenta de nuevo.');
        this.loading.set(false);
      }
    });
  }

  isWatched(symbol: string): boolean {
    return this.watchlistService.isWatched(symbol);
  }

  toggleWatchlist(stock: StockSummary): void {
    if (this.watchlistService.isWatched(stock.symbol)) {
      this.watchlistService.removeFromWatchlist(stock.symbol);
    } else {
      this.watchlistService.addToWatchlist(stock.symbol, stock.name);
    }
  }
}
