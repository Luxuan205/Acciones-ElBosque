import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../core/services/admin.service';
import {
  MarketStatusDto,
  MarketScheduleDto,
  MarketHolidayDto
} from '../../../core/models';

@Component({
  selector: 'app-admin-markets',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-markets.component.html',
  styleUrl: './admin-markets.component.scss'
})
export class AdminMarketsComponent implements OnInit {
  private readonly adminSvc = inject(AdminService);

  // Market status
  marketStatus = signal<MarketStatusDto | null>(null);
  loadingStatus = signal(true);

  // Schedule
  schedule = signal<MarketScheduleDto | null>(null);
  loadingSchedule = signal(true);
  scheduleOpenTime = signal('');
  scheduleCloseTime = signal('');
  selectedDays = signal<string[]>([]);
  savingSchedule = signal(false);
  scheduleSuccess = signal<string | null>(null);
  scheduleError = signal<string | null>(null);

  // Holidays
  holidays = signal<MarketHolidayDto[]>([]);
  loadingHolidays = signal(true);
  newHolidayDate = signal('');
  newHolidayDesc = signal('');
  addingHoliday = signal(false);
  holidayError = signal<string | null>(null);
  holidaySuccess = signal<string | null>(null);

  days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
  dayLabels: Record<string, string> = {
    MONDAY: 'Lunes',
    TUESDAY: 'Martes',
    WEDNESDAY: 'Miércoles',
    THURSDAY: 'Jueves',
    FRIDAY: 'Viernes',
    SATURDAY: 'Sábado',
    SUNDAY: 'Domingo'
  };

  ngOnInit(): void {
    this.loadStatus();
    this.loadSchedule();
    this.loadHolidays();
  }

  loadStatus(): void {
    this.adminSvc.getMarketStatus().subscribe({
      next: data => { this.marketStatus.set(data); this.loadingStatus.set(false); },
      error: () => this.loadingStatus.set(false)
    });
  }

  loadSchedule(): void {
    this.adminSvc.getMarketSchedule().subscribe({
      next: data => {
        this.schedule.set(data);
        this.scheduleOpenTime.set(data.openTime);
        this.scheduleCloseTime.set(data.closeTime);
        this.selectedDays.set([...data.workingDays]);
        this.loadingSchedule.set(false);
      },
      error: () => this.loadingSchedule.set(false)
    });
  }

  loadHolidays(): void {
    this.adminSvc.getHolidays().subscribe({
      next: data => { this.holidays.set(data); this.loadingHolidays.set(false); },
      error: () => this.loadingHolidays.set(false)
    });
  }

  toggleDay(day: string): void {
    const current = this.selectedDays();
    this.selectedDays.set(
      current.includes(day) ? current.filter(d => d !== day) : [...current, day]
    );
  }

  isDaySelected(day: string): boolean {
    return this.selectedDays().includes(day);
  }

  saveSchedule(): void {
    if (!this.scheduleOpenTime() || !this.scheduleCloseTime()) {
      this.scheduleError.set('La hora de apertura y cierre son requeridas.');
      return;
    }
    if (this.selectedDays().length === 0) {
      this.scheduleError.set('Selecciona al menos un día hábil.');
      return;
    }
    this.savingSchedule.set(true);
    this.scheduleError.set(null);
    const req: MarketScheduleDto = {
      openTime: this.scheduleOpenTime(),
      closeTime: this.scheduleCloseTime(),
      workingDays: this.selectedDays()
    };
    this.adminSvc.updateMarketSchedule(req).subscribe({
      next: updated => {
        this.schedule.set(updated);
        this.scheduleSuccess.set('Horario de mercado actualizado exitosamente.');
        this.savingSchedule.set(false);
        setTimeout(() => this.scheduleSuccess.set(null), 4000);
      },
      error: err => {
        this.scheduleError.set(err?.error?.message || 'Error al guardar el horario.');
        this.savingSchedule.set(false);
      }
    });
  }

  addHoliday(): void {
    if (!this.newHolidayDate() || !this.newHolidayDesc().trim()) {
      this.holidayError.set('La fecha y descripción son requeridas.');
      return;
    }
    this.addingHoliday.set(true);
    this.holidayError.set(null);
    this.adminSvc.addHoliday({ date: this.newHolidayDate(), description: this.newHolidayDesc().trim() }).subscribe({
      next: holiday => {
        this.holidays.set([...this.holidays(), holiday]);
        this.newHolidayDate.set('');
        this.newHolidayDesc.set('');
        this.holidaySuccess.set('Festivo agregado exitosamente.');
        this.addingHoliday.set(false);
        setTimeout(() => this.holidaySuccess.set(null), 4000);
      },
      error: err => {
        this.holidayError.set(err?.error?.message || 'Error al agregar el festivo.');
        this.addingHoliday.set(false);
      }
    });
  }

  deleteHoliday(holiday: MarketHolidayDto): void {
    if (!confirm(`¿Eliminar el festivo del ${holiday.date} (${holiday.description})?`)) return;
    this.adminSvc.deleteHoliday(holiday.id).subscribe({
      next: () => {
        this.holidays.set(this.holidays().filter(h => h.id !== holiday.id));
        this.holidaySuccess.set('Festivo eliminado.');
        setTimeout(() => this.holidaySuccess.set(null), 3000);
      },
      error: () => alert('Error al eliminar el festivo.')
    });
  }
}
