import { Component, ElementRef, HostListener, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { ProfileResponse } from '../../../core/models';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent implements OnInit {
  private readonly auth   = inject(AuthService);
  private readonly router = inject(Router);
  private readonly el     = inject(ElementRef);

  profile     = signal<ProfileResponse | null>(null);
  adminOpen   = signal(false);
  brokerOpen  = signal(false);
  mobileOpen  = signal(false);
  userMenuOpen = signal(false);

  get loggedIn(): boolean { return this.auth.isLoggedIn(); }
  get isAdmin(): boolean  { return this.auth.isAdmin(); }
  get isBroker(): boolean { return this.auth.isBroker(); }

  ngOnInit(): void {
    if (this.loggedIn) {
      this.auth.getProfile().subscribe({
        next: p => this.profile.set(p),
        error: () => {}
      });
    }
  }

  logout(): void {
    this.closeAll();
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  toggleAdmin(): void {
    this.adminOpen.update(v => !v);
    this.brokerOpen.set(false);
    this.userMenuOpen.set(false);
  }

  toggleBroker(): void {
    this.brokerOpen.update(v => !v);
    this.adminOpen.set(false);
    this.userMenuOpen.set(false);
  }

  toggleUserMenu(): void {
    this.userMenuOpen.update(v => !v);
    this.adminOpen.set(false);
    this.brokerOpen.set(false);
  }

  toggleMobile(): void {
    this.mobileOpen.update(v => !v);
  }

  closeMenus(): void {
    this.adminOpen.set(false);
    this.brokerOpen.set(false);
  }

  closeAll(): void {
    this.adminOpen.set(false);
    this.brokerOpen.set(false);
    this.userMenuOpen.set(false);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.closeAll();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.el.nativeElement.contains(event.target)) {
      this.closeAll();
    }
  }
}
