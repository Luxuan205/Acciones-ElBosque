import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService } from '../../core/services/notification.service';
import { NotificationDto } from '../../core/models';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notifications.component.html',
  styleUrls: ['./notifications.component.scss'],
})
export class NotificationsComponent implements OnInit {
  private notificationService = inject(NotificationService);

  notifications = signal<NotificationDto[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  activeFilter = signal<'ALL' | 'UNREAD'>('ALL');

  get filtered(): NotificationDto[] {
    if (this.activeFilter() === 'UNREAD') {
      return this.notifications().filter(n => !n.read);
    }
    return this.notifications();
  }

  get unreadCount(): number {
    return this.notifications().filter(n => !n.read).length;
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.notificationService.getNotifications().subscribe({
      next: data => {
        this.notifications.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las notificaciones.');
        this.loading.set(false);
      },
    });
  }

  markRead(id: number): void {
    this.notificationService.markRead(id).subscribe({
      next: () => {
        this.notifications.update(list =>
          list.map(n => (n.id === id ? { ...n, read: true } : n))
        );
      },
    });
  }

  setFilter(filter: 'ALL' | 'UNREAD'): void {
    this.activeFilter.set(filter);
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('es-CO', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
}
