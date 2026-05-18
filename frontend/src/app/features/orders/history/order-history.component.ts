import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { OrderService } from '../../../core/services/order.service';
import { Order } from '../../../core/models';

@Component({
  selector: 'app-order-history',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './order-history.component.html',
  styleUrl: './order-history.component.scss'
})
export class OrderHistoryComponent implements OnInit {
  private readonly orderSvc = inject(OrderService);

  loading = signal(true);
  error = signal<string | null>(null);
  orders = signal<Order[]>([]);
  filterStatus = signal<string>('ALL');
  filterType = signal<string>('ALL');
  cancelError = signal<string | null>(null);
  cancelSuccess = signal<string | null>(null);
  sortDesc = signal(true);

  readonly statusOptions = ['ALL', 'PENDING', 'OPEN', 'EXECUTED', 'CANCELLED', 'REJECTED'];
  readonly typeOptions = ['ALL', 'BUY', 'SELL', 'LIMIT', 'STOP_LOSS'];

  filteredOrders = computed(() => {
    let list = [...this.orders()];

    const status = this.filterStatus();
    if (status !== 'ALL') {
      list = list.filter(o => o.status === status);
    }

    const type = this.filterType();
    if (type !== 'ALL') {
      list = list.filter(o => o.orderType.includes(type));
    }

    list.sort((a, b) => {
      const diff = new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      return this.sortDesc() ? diff : -diff;
    });

    return list;
  });

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading.set(true);
    this.error.set(null);
    this.orderSvc.getOrders().subscribe({
      next: data => {
        this.orders.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar el historial de órdenes.');
        this.loading.set(false);
      }
    });
  }

  setFilterStatus(status: string): void {
    this.filterStatus.set(status);
  }

  setFilterType(type: string): void {
    this.filterType.set(type);
  }

  toggleSort(): void {
    this.sortDesc.update(v => !v);
  }

  canCancel(order: Order): boolean {
    return order.status === 'PENDING' || order.status === 'OPEN';
  }

  onCancel(order: Order): void {
    if (!confirm(`¿Confirma cancelar la orden #${order.id} (${order.symbol})?`)) return;
    this.cancelError.set(null);
    this.cancelSuccess.set(null);

    this.orderSvc.cancelOrder(order.id).subscribe({
      next: res => {
        this.cancelSuccess.set(res.message || `Orden #${order.id} cancelada.`);
        this.orders.update(list =>
          list.map(o => o.id === order.id ? { ...o, status: 'CANCELLED' } : o)
        );
      },
      error: () => {
        this.cancelError.set(`Error al cancelar la orden #${order.id}.`);
      }
    });
  }

  statusBadge(status: string): string {
    switch (status) {
      case 'EXECUTED': return 'badge-green';
      case 'PENDING':
      case 'OPEN': return 'badge-yellow';
      case 'CANCELLED':
      case 'REJECTED': return 'badge-red';
      default: return 'badge-cyan';
    }
  }

  formatCOP(value: number): string {
    return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(value);
  }
}
