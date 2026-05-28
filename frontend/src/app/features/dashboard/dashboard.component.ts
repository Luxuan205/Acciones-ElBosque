import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { NgApexchartsModule } from 'ng-apexcharts';
import {
  ApexAxisChartSeries,
  ApexChart,
  ApexDataLabels,
  ApexGrid,
  ApexStroke,
  ApexTooltip,
  ApexXAxis,
  ApexYAxis,
  ApexNonAxisChartSeries,
  ApexPlotOptions,
  ApexLegend,
  ApexFill,
} from 'ng-apexcharts';
import { AuthService } from '../../core/services/auth.service';
import { PortfolioService } from '../../core/services/portfolio.service';
import { OrderService } from '../../core/services/order.service';
import { WatchlistService } from '../../core/services/watchlist.service';
import { MarketService } from '../../core/services/market.service';
import {
  BalanceSummaryResponse, ProfileResponse, Order,
  PortfolioPositionsResponse, StockSummary, WatchlistItem,
  PortfolioHistoryResponse
} from '../../core/models';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

export interface SparklineRow {
  item: WatchlistItem;
  detail: StockSummary | null;
  chartSeries: { data: number[] }[];
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, NgApexchartsModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly portfolio = inject(PortfolioService);
  private readonly orderSvc = inject(OrderService);
  private readonly watchlistSvc = inject(WatchlistService);
  private readonly marketSvc = inject(MarketService);

  profile = signal<ProfileResponse | null>(null);
  balance = signal<BalanceSummaryResponse | null>(null);
  positions = signal<PortfolioPositionsResponse | null>(null);
  orders = signal<Order[]>([]);
  sparklines = signal<SparklineRow[]>([]);

  loadingBalance = signal(true);
  loadingOrders = signal(true);
  loadingProfile = signal(true);
  loadingCharts = signal(true);
  hasChartData = signal(false);

  today = new Date();

  readonly periods = [
    { label: '30D', days: 30 },
    { label: '7D',  days: 7  },
    { label: '3M',  days: 90 },
    { label: '1A',  days: 365 },
  ];
  selectedPeriod = signal<string>('30D');

  // ── Portfolio line chart ───────────────────────────────────
  portfolioSeries: ApexAxisChartSeries = [];
  portfolioChart: ApexChart = {
    type: 'area',
    height: 220,
    toolbar: { show: false },
    sparkline: { enabled: false },
    animations: { enabled: true, speed: 600 },
    background: 'transparent',
  };
  portfolioStroke: ApexStroke = { curve: 'smooth', width: 2.2 };
  portfolioFill: ApexFill = {
    type: 'gradient',
    gradient: {
      shadeIntensity: 1,
      opacityFrom: 0.22,
      opacityTo: 0.0,
      stops: [0, 100],
      colorStops: [
        { offset: 0, color: '#3F7A4E', opacity: 0.22 },
        { offset: 100, color: '#3F7A4E', opacity: 0 },
      ],
    },
  };
  portfolioXAxis: ApexXAxis = {
    type: 'category',
    labels: { style: { colors: '#7E8A77', fontSize: '11px' }, rotate: 0 },
    axisBorder: { show: false },
    axisTicks: { show: false },
    tickAmount: 6,
  };
  portfolioYAxis: ApexYAxis = {
    labels: {
      style: { colors: '#7E8A77', fontSize: '11px' },
      formatter: (v: number) => this.formatCurrencyShort(v),
    },
  };
  portfolioGrid: ApexGrid = {
    borderColor: '#E6DDC6',
    strokeDashArray: 5,
    xaxis: { lines: { show: false } },
  };
  portfolioTooltip: ApexTooltip = {
    theme: 'light',
    y: { formatter: (v: number) => this.formatCurrency(v) },
  };

  // ── Donut chart ───────────────────────────────────────────
  donutSeries: ApexNonAxisChartSeries = [];
  donutChart: ApexChart = {
    type: 'donut',
    height: 220,
    toolbar: { show: false },
    background: 'transparent',
    animations: { enabled: true, speed: 600 },
  };
  donutLabels: string[] = [];
  donutColors: string[] = ['#3F7A4E', '#D89154', '#5B7D9A', '#B5743C', '#C6DCC1'];
  donutPlotOptions: ApexPlotOptions = {
    pie: {
      donut: {
        size: '68%',
        labels: {
          show: true,
          total: {
            show: true,
            label: 'Total',
            color: '#7E8A77',
            fontSize: '12px',
            formatter: (w) => {
              const s = w.globals.seriesTotals.reduce((a: number, b: number) => a + b, 0);
              return this.formatCurrencyShort(s);
            },
          },
        },
      },
    },
  };
  donutLegend: ApexLegend = {
    position: 'bottom',
    labels: { colors: '#7E8A77' },
    fontSize: '11px',
  };
  donutDataLabels: ApexDataLabels = { enabled: false };
  donutTooltip: ApexTooltip = {
    theme: 'light',
    y: { formatter: (v: number) => this.formatCurrencyShort(v) },
  };

  // ── Sparkline options (shared) ────────────────────────────
  sparkChart: ApexChart = {
    type: 'area',
    height: 40,
    sparkline: { enabled: true },
    animations: { enabled: false },
    background: 'transparent',
  };
  sparkStroke: ApexStroke = { curve: 'smooth', width: 1.6 };
  sparkTooltip: ApexTooltip = { enabled: false };

  get activeOrders(): Order[] {
    return this.orders().filter(o => ['PENDING', 'OPEN', 'PARTIALLY_FILLED'].includes(o.status));
  }

  ngOnInit(): void {
    this.auth.getProfile().subscribe({
      next: p => { this.profile.set(p); this.loadingProfile.set(false); },
      error: ()  => this.loadingProfile.set(false)
    });

    this.orderSvc.getOrders().subscribe({
      next: o => { this.orders.set(o); this.loadingOrders.set(false); },
      error: ()  => this.loadingOrders.set(false)
    });

    forkJoin({
      balance: this.portfolio.getBalance().pipe(catchError(() => of(null))),
      positions: this.portfolio.getPositions().pipe(catchError(() => of(null))),
    }).subscribe(({ balance, positions }) => {
      this.balance.set(balance);
      this.positions.set(positions);
      this.loadingBalance.set(false);
      this.buildDonutChart(positions);
      this.portfolio.getPortfolioHistory('30D').pipe(
        catchError(() => of({ points: [] } as PortfolioHistoryResponse))
      ).subscribe(history => {
        if (history.points.length > 0) {
          this.buildPortfolioChartFromHistory(history);
          this.hasChartData.set(true);
        }
        this.loadingCharts.set(false);
      });
    });

    this.buildSparklines();
  }

  selectPeriod(label: string): void {
    this.selectedPeriod.set(label);
    this.portfolio.getPortfolioHistory(label).pipe(
      catchError(() => of({ points: [] } as PortfolioHistoryResponse))
    ).subscribe(history => {
      if (history.points.length > 0) {
        this.buildPortfolioChartFromHistory(history);
        this.hasChartData.set(true);
      } else {
        this.hasChartData.set(false);
      }
    });
  }

  private buildPortfolioChartFromHistory(history: PortfolioHistoryResponse): void {
    const categories = history.points.map(p => {
      const [y, m, d] = p.date.split('-').map(Number);
      return new Date(y, m - 1, d).toLocaleDateString('es-CO', { day: '2-digit', month: 'short' });
    });
    const values = history.points.map(p => Math.round(p.totalValue));
    this.portfolioSeries = [{ name: 'Valor portafolio', data: values, color: '#4aaa60' }];
    this.portfolioXAxis = { ...this.portfolioXAxis, categories };
  }

  private buildDonutChart(positions: PortfolioPositionsResponse | null): void {
    const pos = positions?.positions ?? [];
    if (pos.length === 0) {
      this.donutSeries = [1];
      this.donutLabels = ['Sin posiciones'];
      return;
    }
    this.donutSeries = pos.map(p => Math.round(p.marketValue));
    this.donutLabels = pos.map(p => p.symbol);
  }

  private buildSparklines(): void {
    const items = this.watchlistSvc.getWatchlist().slice(0, 6);
    if (items.length === 0) return;

    const requests = items.map(item =>
      forkJoin({
        detail: this.marketSvc.getStock(item.symbol).pipe(catchError(() => of(null))),
        intraday: this.marketSvc.getIntraday(item.symbol).pipe(catchError(() => of(null))),
      })
    );

    forkJoin(requests).subscribe(results => {
      const rows: SparklineRow[] = items.map((item, i) => {
        const detail = results[i].detail as StockSummary | null;
        const intraday = results[i].intraday as { points: { price: number }[] } | null;

        let chartData: number[];
        if (intraday && intraday.points.length > 1) {
          chartData = intraday.points.map(p => Math.round(Number(p.price)));
        } else if (detail) {
          chartData = Array(10).fill(Math.round(detail.currentPrice));
        } else {
          chartData = [];
        }

        return { item, detail, chartSeries: [{ data: chartData }] };
      });
      this.sparklines.set(rows);
    });
  }

  sparkColor(row: SparklineRow): string {
    return (row.detail?.dayChangePct ?? 0) >= 0 ? '#4aaa60' : '#ff4757';
  }

  sparkFill(row: SparklineRow): ApexFill {
    const color = this.sparkColor(row);
    return {
      type: 'gradient',
      gradient: {
        shadeIntensity: 1,
        opacityFrom: 0.15,
        opacityTo: 0,
        colorStops: [
          { offset: 0, color, opacity: 0.15 },
          { offset: 100, color, opacity: 0 },
        ],
      },
    };
  }

  formatCurrency(value: number | null | undefined): string {
    if (value == null) return '—';
    return new Intl.NumberFormat('es-CO', {
      style: 'currency', currency: 'COP', minimumFractionDigits: 0
    }).format(value);
  }

  formatCurrencyShort(value: number): string {
    if (Math.abs(value) >= 1_000_000)
      return `$${(value / 1_000_000).toFixed(1)}M`;
    if (Math.abs(value) >= 1_000)
      return `$${(value / 1_000).toFixed(0)}K`;
    return `$${value.toFixed(0)}`;
  }

  formatPercent(value: number | null | undefined): string {
    if (value == null) return '—';
    return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`;
  }
}
