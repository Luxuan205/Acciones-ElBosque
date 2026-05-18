import { Injectable } from '@angular/core';
import { WatchlistItem } from '../models';

@Injectable({ providedIn: 'root' })
export class WatchlistService {
  private readonly STORAGE_KEY = 'ab_watchlist';

  getWatchlist(): WatchlistItem[] {
    try {
      const raw = localStorage.getItem(this.STORAGE_KEY);
      return raw ? (JSON.parse(raw) as WatchlistItem[]) : [];
    } catch {
      return [];
    }
  }

  addToWatchlist(symbol: string, name: string): void {
    const list = this.getWatchlist();
    if (!list.find(item => item.symbol === symbol)) {
      list.push({ symbol, name, addedAt: new Date().toISOString() });
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(list));
    }
  }

  removeFromWatchlist(symbol: string): void {
    const list = this.getWatchlist().filter(item => item.symbol !== symbol);
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(list));
  }

  isWatched(symbol: string): boolean {
    return this.getWatchlist().some(item => item.symbol === symbol);
  }
}
