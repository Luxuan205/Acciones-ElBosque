import { Component, inject, OnInit, signal } from '@angular/core';
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
import {
  BalanceSummaryResponse, ProfileResponse, Order,
  PortfolioPositionsResponse, StockSummary, WatchlistItem
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

  profile = signal<ProfileResponse | null>(null);
  balance = signal<BalanceSummaryResponse | null>(null);
  positions = signal<PortfolioPositionsResponse | null>(null);
  orders = signal<Order[]>([]);
  sparklines = signal<SparklineRow[]>([]);

  loadingBalance = signal(true);
  loadingOrders = signal(true);
  loadingProfile = signal(true);
  loadingCharts = signal(true);

  today = new Date();

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
      opacityFrom: 0.2,
      opacityTo: 0.0,
      stops: [0, 100],
      colorStops: [
        { offset: 0, color: '#3F7A4E', opacity: 0.2 },
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
      this.buildPortfolioChart(balance);
      this.buildDonutChart(positions);
      this.loadingCharts.set(false);
    });

    this.buildSparklines();
  }

  private buildPortfolioChart(balance: BalanceSummaryResponse | null): void {
    const base = balance?.totalPortfolioValue ?? 1_000_000;
    const days = 30;
    const categories: string[] = [];
    const values: number[] = [];
    let current = base * 0.88;
    const now = new Date();

    for (let i = days - 1; i >= 0; i--) {
      const d = new Date(now);
      d.setDate(d.getDate() - i);
      categories.push(d.toLocaleDateString('es-CO', { day: '2-digit', month: 'short' }));
      current = current * (1 + (Math.random() * 0.022 - 0.006));
      if (i === 0) current = base;
      values.push(Math.round(current));
    }

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
    const rows: SparklineRow[] = items.map(item => {
      const base = 10000 + Math.random() * 90000;
      const data: number[] = [];
      let v = base;
      for (let i = 0; i < 20; i++) {
        v = v * (1 + (Math.random() * 0.04 - 0.015));
        data.push(Math.round(v));
      }
      const change = ((data[data.length - 1] - data[0]) / data[0]) * 100;
      return {
        item,
        detail: {
          symbol: item.symbol,
          name: item.name,
          currentPrice: data[data.length - 1],
          dayChangePct: parseFloat(change.toFixed(2)),
          volume: Math.round(Math.random() * 1_000_000),
        },
        chartSeries: [{ data }],
      };
    });
    this.sparklines.set(rows);
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
