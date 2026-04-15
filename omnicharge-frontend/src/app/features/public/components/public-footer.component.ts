import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-public-footer',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <footer class="bg-gradient-to-br from-slate-900 via-slate-950 to-indigo-950 text-slate-400 py-10 border-t border-slate-900 relative overflow-hidden">
      <!-- Glow effect for the footer -->
      <div class="absolute bottom-0 right-0 w-96 h-96 bg-indigo-600/10 rounded-full blur-3xl pointer-events-none"></div>

      <div class="max-w-[1400px] mx-auto px-6 relative z-10">
        
        <!-- Top Section (4 Columns) -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-12 lg:gap-8 mb-16">
          
          <!-- Col 1: Brand -->
          <div class="lg:pr-8">
            <div class="flex items-center gap-3 mb-6">
              <div class="w-8 h-8 rounded-lg bg-indigo-600 flex items-center justify-center">
                <mat-icon class="!text-white !text-[18px] leading-none">bolt</mat-icon>
              </div>
              <span class="text-xl font-bold text-white tracking-tight">OMNICHARGE</span>
            </div>
            <p class="text-slate-400 text-sm leading-relaxed mb-6 font-medium">
              Lightning fast recharges for a billion Indians. Enterprise-grade microservices for your everyday transactions.
            </p>
          </div>

          <!-- Col 2: Operators -->
          <div>
            <h4 class="text-white font-bold text-sm tracking-widest uppercase mb-4">Supported Networks</h4>
            <div class="flex flex-col gap-3">
              <a href="#" class="text-sm font-medium text-slate-400 hover:text-white transition-colors no-underline">Airtel Pre-paid</a>
              <a href="#" class="text-sm font-medium text-slate-400 hover:text-white transition-colors no-underline">Jio Digital Life</a>
              <a href="#" class="text-sm font-medium text-slate-400 hover:text-white transition-colors no-underline">Vodafone Idea (Vi)</a>
              <a href="#" class="text-sm font-medium text-slate-400 hover:text-white transition-colors no-underline">BSNL Mobile</a>
            </div>
          </div>

          <!-- Col 3: Company -->
          <div>
            <h4 class="text-white font-bold text-sm tracking-widest uppercase mb-4">Company</h4>
            <div class="flex flex-col gap-3">
              <a href="#" class="text-sm font-medium text-slate-400 hover:text-white transition-colors no-underline">About Us</a>
              <a href="#" class="text-sm font-medium text-slate-400 hover:text-white transition-colors no-underline">Careers at OmniCharge</a>
              <a href="#" class="text-sm font-medium text-slate-400 hover:text-white transition-colors no-underline">Terms of Service</a>
              <a href="#" class="text-sm font-medium text-slate-400 hover:text-white transition-colors no-underline">Privacy Policy</a>
            </div>
          </div>

          <!-- Col 4: Support -->
          <div>
            <h4 class="text-white font-bold text-sm tracking-widest uppercase mb-4">Support</h4>
            <div class="flex flex-col gap-3">
              <a href="#" class="text-sm font-medium text-slate-400 hover:text-white transition-colors no-underline">24/7 Help Center</a>
              <a href="#" class="text-sm font-medium text-slate-400 hover:text-white transition-colors no-underline">Report Fraud</a>
              <a href="#" class="text-sm font-medium text-slate-400 hover:text-white transition-colors no-underline">Transaction Status</a>
              <a href="#" class="text-sm font-medium text-slate-400 hover:text-white transition-colors no-underline">API Documentation</a>
            </div>
          </div>

        </div>

        <!-- Bottom Section -->
        <div class="pt-8 border-t border-slate-800/80 flex flex-col md:flex-row items-center justify-between gap-6">
          <div class="flex items-center gap-4">
            <img src="assets/images/hero.png" alt="Mini Mascot" class="w-12 h-12 object-contain hover:-translate-y-1 transition-transform cursor-pointer drop-shadow-[0_0_8px_rgba(99,102,241,0.5)] bg-white rounded-full p-0.5">
            <p class="text-sm font-medium text-slate-500">Copyright &copy; 2026 OmniCharge. All rights reserved.</p>
          </div>
          <div class="flex items-center gap-5 text-slate-500">
            <a href="#" class="hover:text-white transition-colors no-underline"><mat-icon class="!w-7 !h-7 !text-[28px]">language</mat-icon></a>
            <a href="#" class="hover:text-white transition-colors flex items-center justify-center font-bold text-2xl no-underline">X</a>
            <a href="#" class="hover:text-white transition-colors flex items-center justify-center font-bold text-2xl no-underline">in</a>
          </div>
        </div>

      </div>
    </footer>
  `
})
export class PublicFooterComponent { }
