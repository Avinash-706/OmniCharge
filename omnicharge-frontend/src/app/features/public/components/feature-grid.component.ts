import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-feature-grid',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <section class="py-24 bg-white relative">
      <div class="max-w-[1200px] mx-auto px-6 relative z-10">
        
        <!-- Header -->
        <div class="text-center max-w-3xl mx-auto mb-16">
          <h2 class="text-xl md:text-2xl font-bold tracking-tight text-slate-400 mb-2">Built for the Indian Scale</h2>
          <h3 class="text-4xl md:text-5xl font-black text-slate-900 tracking-tight">Enterprise Infrastructure. <br> Consumer Simplicity.</h3>
        </div>

        <!-- 3-Column Grid -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-8 text-center md:text-left">
          
          <!-- Feature 1 -->
          <div class="group p-8 rounded-[2rem] bg-slate-50 hover:bg-white border border-slate-100 hover:border-indigo-100 shadow-sm hover:shadow-xl hover:shadow-indigo-500/10 hover:-translate-y-2 transition-all duration-300">
            <div class="w-14 h-14 bg-indigo-100 text-indigo-600 rounded-2xl flex items-center justify-center mb-8 mx-auto md:mx-0 group-hover:scale-110 group-hover:bg-indigo-600 group-hover:text-white shadow-xl shadow-indigo-600/0 group-hover:shadow-indigo-600/30 transition-all duration-300">
              <mat-icon class="!text-[28px] !w-7 !h-7 leading-none">dynamic_form</mat-icon>
            </div>
            <h4 class="text-2xl font-black text-slate-900 mb-3 tracking-tight">Instant SAGA Processing</h4>
            <p class="text-slate-500 font-medium leading-relaxed">
              Our asynchronous RabbitMQ driven microservices ensure 99.9% of recharges clear in under 2 seconds. No more pending screens.
            </p>
          </div>

          <!-- Feature 2 -->
          <div class="group p-8 rounded-[2rem] bg-slate-50 hover:bg-white border border-slate-100 hover:border-emerald-100 shadow-sm hover:shadow-xl hover:shadow-emerald-500/10 hover:-translate-y-2 transition-all duration-300">
            <div class="w-14 h-14 bg-emerald-100 text-emerald-600 rounded-2xl flex items-center justify-center mb-8 mx-auto md:mx-0 group-hover:scale-110 group-hover:bg-emerald-600 group-hover:text-white shadow-xl shadow-emerald-600/0 group-hover:shadow-emerald-600/30 transition-all duration-300">
              <mat-icon class="!text-[28px] !w-7 !h-7 leading-none">verified_user</mat-icon>
            </div>
            <h4 class="text-2xl font-black text-slate-900 mb-3 tracking-tight">Bank-Grade Security</h4>
            <p class="text-slate-500 font-medium leading-relaxed">
              SSL/TLS wrapped endpoints processing transactions through highly scrutinized API Gateways and immutable audit logs.
            </p>
          </div>

          <!-- Feature 3 -->
          <div class="group p-8 rounded-[2rem] bg-slate-50 hover:bg-white border border-slate-100 hover:border-amber-100 shadow-sm hover:shadow-xl hover:shadow-amber-500/10 hover:-translate-y-2 transition-all duration-300">
            <div class="w-14 h-14 bg-amber-100 text-amber-600 rounded-2xl flex items-center justify-center mb-8 mx-auto md:mx-0 group-hover:scale-110 group-hover:bg-amber-500 group-hover:text-white shadow-xl shadow-amber-500/0 group-hover:shadow-amber-500/30 transition-all duration-300">
              <mat-icon class="!text-[28px] !w-7 !h-7 leading-none">support_agent</mat-icon>
            </div>
            <h4 class="text-2xl font-black text-slate-900 mb-3 tracking-tight">24/7 Sentry Support</h4>
            <p class="text-slate-500 font-medium leading-relaxed">
              If an operator fails to issue the plan, our distributed sagas automatically trigger split-second reversals to refund you instantly.
            </p>
          </div>

        </div>
      </div>
    </section>
  `
})
export class FeatureGridComponent { }
