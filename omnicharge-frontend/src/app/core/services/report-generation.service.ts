import { Injectable, inject } from '@angular/core';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { HttpClient } from '@angular/common/http';
import { AdminDashboardStateService, DashboardAnalytics } from './admin-dashboard-state.service';
import { AdminPaymentService, TransactionResponse, PageResponse } from './admin-payment.service';
import { AdminRechargeService, RechargeResponse } from './admin-recharge.service';
import { AdminUserService, AdminUserProfile } from './admin-user.service';
import { TokenService } from '../auth/token.service';
import { environment } from '../../../environments/environment';
import { firstValueFrom, filter } from 'rxjs';
import { map } from 'rxjs/operators';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class ReportGenerationService {
  private http = inject(HttpClient);
  private dashboardState = inject(AdminDashboardStateService);
  private paymentService = inject(AdminPaymentService);
  private rechargeService = inject(AdminRechargeService);
  private userService = inject(AdminUserService);
  private tokenService = inject(TokenService);
  private apiUrl = `${environment.apiGatewayUrl}/api/admin/reports`;

  /**
   * Download PDF delegated to generateReportContent (see below).
   * The actual downloadPdf() implementation is after generateReportContent.
   */


  /**
   * Email the Executive Summary to the hardcoded admin email.
   * Generates a PDF, converts it to Base64, and sends it as an attachment.
   */
  async emailReport(): Promise<string> {
    const analytics = await firstValueFrom(
      this.dashboardState.getAnalytics().pipe(filter((a): a is DashboardAnalytics => a !== null))
    );
    if (!analytics) throw new Error('No analytics data available. Refresh the dashboard first.');

    // 1. Generate the same professional PDF as downloadPdf()
    const doc = new jsPDF('p', 'mm', 'a4');
    await this.generateReportContent(doc, analytics);
    
    // 2. Convert to Base64 (strip the Data URI prefix)
    const pdfDataUri = doc.output('datauristring');
    const pdfBase64 = pdfDataUri.split('base64,')[1];

    const adminEmail = 'avunashdhanuka@gmail.com';

    const response = await firstValueFrom(
      this.http.post<ApiResponse<string>>(`${this.apiUrl}/send-email`, {
        adminEmail,
        reportSubject: `OmniCharge Executive Confidential Report — ${new Date().toLocaleDateString('en-IN', { year: 'numeric', month: 'long', day: 'numeric' })}`,
        pdfBase64: pdfBase64
      }).pipe(map(r => r.message))
    );

    console.log('✅ Executive Report successfully dispatched to avunashdhanuka@gmail.com');
    return response;
  }

  /**
   * Refactored PDF generation logic for reuse between download and email
   */
  private async generateReportContent(doc: any, analytics: DashboardAnalytics): Promise<void> {
    const pageWidth = doc.internal.pageSize.getWidth();
    const pageHeight = doc.internal.pageSize.getHeight();
    const now = new Date();
    const timestamp = now.toLocaleString('en-IN', { dateStyle: 'full', timeStyle: 'short' });

    // ============ PAGE 1: HEADER + KPIs ============
    doc.setFillColor(30, 41, 59);
    doc.rect(0, 0, pageWidth, 35, 'F');
    doc.setFillColor(99, 102, 241);
    doc.rect(0, 35, pageWidth, 2, 'F');

    doc.setTextColor(255, 255, 255);
    doc.setFontSize(18);
    doc.setFont('helvetica', 'bold');
    doc.text('OMNICHARGE EXECUTIVE BI', 15, 18);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    doc.text('SYSTEM PERFORMANCE & REVENUE AUDIT', 15, 25);
    doc.setFontSize(8);
    doc.setTextColor(148, 163, 184);
    doc.text(timestamp, 15, 31);
    doc.text('CLASSIFIED: CONFIDENTIAL', pageWidth - 15, 31, { align: 'right' });

    let y = 46;
    doc.setFontSize(11);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(15, 23, 42);
    doc.text('KEY PERFORMANCE INDICATORS', 15, y);
    y += 6;

    const { payments, recharges, users, plans } = analytics;
    const fmt = (v: number) => `INR ${v.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;

    const kpiData = [
      ['Gross Revenue', fmt(payments.grossRevenue), 'Today Revenue', fmt(payments.todayRevenue)],
      ['Month Revenue', fmt(payments.monthRevenue), 'Last Month', fmt(payments.lastMonthRevenue)],
      ['MoM Growth', `${payments.revenueGrowthPercentage.toFixed(2)}%`, 'Success Rate', `${recharges.successRate.toFixed(2)}%`],
      ['Total Users', users.totalUsers.toLocaleString(), 'New (30d)', users.newUsersThisMonth.toLocaleString()],
      ['Total Recharges', recharges.totalRecharges.toLocaleString(), 'Active Plans', plans.activePlans.toLocaleString()]
    ];

    autoTable(doc, {
      startY: y,
      body: kpiData,
      theme: 'grid',
      styles: { fontSize: 8, cellPadding: 2.5 },
      columnStyles: { 
        0: { fontStyle: 'bold', fillColor: [248, 250, 252] },
        2: { fontStyle: 'bold', fillColor: [248, 250, 252] }
      },
      margin: { left: 15, right: 15 }
    });
    y = (doc as any).lastAutoTable.finalY + 12;

    // TOP SPENDERS
    doc.setFont('helvetica', 'bold');
    doc.text('WHALE LEADERBOARD (TOP SPENDERS)', 15, y);
    y += 5;
    const topSpendersData = (payments.topSpenders || []).slice(0, 15).map((s, i) => [
      i + 1, s.fullName || 'User #' + s.userId, s.userEmail || '—', s.transactionCount, fmt(s.totalSpent)
    ]);
    autoTable(doc, {
      startY: y,
      head: [['Rank', 'Full Name', 'Email Address', 'Txns', 'Total Lifetime Spend']],
      body: topSpendersData,
      theme: 'striped',
      headStyles: { fillColor: [79, 70, 229], fontSize: 8 },
      styles: { fontSize: 7 },
      margin: { left: 15, right: 15 }
    });
    y = (doc as any).lastAutoTable.finalY + 12;

    // ============ USERS TABLE ============
    if (y > pageHeight - 40) { doc.addPage(); y = 20; }
    doc.setFont('helvetica', 'bold');
    doc.text('LATEST REGISTERED USERS', 15, y);
    y += 5;
    try {
      const allUsers = await firstValueFrom(this.userService.getAllUsers());
      const userData = allUsers.slice(0, 30).map(u => [
        u.id, u.fullName, u.email, u.mobileNumber || '—', u.role, u.isActive ? 'ACTIVE' : 'SUSPENDED'
      ]);
      autoTable(doc, {
        startY: y,
        head: [['ID', 'Name', 'Email', 'Mobile', 'Role', 'Status']],
        body: userData,
        theme: 'striped',
        headStyles: { fillColor: [30, 41, 59], fontSize: 8 },
        styles: { fontSize: 7 },
        margin: { left: 15, right: 15 }
      });
      y = (doc as any).lastAutoTable.finalY + 12;
    } catch (e) {
      doc.text('User directory data unavailable.', 15, y);
      y += 10;
    }

    // ============ PAYMENTS TABLE ============
    if (y > pageHeight - 40) { doc.addPage(); y = 20; }
    doc.setFont('helvetica', 'bold');
    doc.text('RECENT PAYMENTS', 15, y);
    y += 5;
    try {
      const paymentsPage: any = await firstValueFrom(this.paymentService.getAllTransactions(0, 30));
      const paymentData = (paymentsPage.content || []).map((t: any) => [
        t.transactionId?.substring(0, 12) + '...' || '—',
        t.userEmail || '—',
        t.amount?.toFixed(2) || '0.00',
        t.paymentMethod || '—',
        t.status || '—',
        t.createdAt ? new Date(t.createdAt).toLocaleDateString('en-IN') : '—'
      ]);
      autoTable(doc, {
        startY: y,
        head: [['Txn ID', 'User', 'Amount (INR)', 'Method', 'Status', 'Date']],
        body: paymentData,
        theme: 'striped',
        headStyles: { fillColor: [16, 185, 129], fontSize: 8 },
        styles: { fontSize: 7 },
        margin: { left: 15, right: 15 }
      });
      y = (doc as any).lastAutoTable.finalY + 12;
    } catch (e) {
      doc.text('Payment data unavailable.', 15, y);
      y += 10;
    }

    // ============ RECHARGES TABLE ============
    if (y > pageHeight - 40) { doc.addPage(); y = 20; }
    doc.setFont('helvetica', 'bold');
    doc.text('RECENT RECHARGES', 15, y);
    y += 5;
    try {
      const rechargesPage: any = await firstValueFrom(this.rechargeService.getAllRecharges(0, 30));
      const rechargeData = (rechargesPage.content || []).map((r: any) => [
        r.id?.toString()?.substring(0, 12) || '—',
        r.mobileNumber || '—',
        r.operatorName || '—',
        r.planName || '—',
        r.amount?.toFixed(2) || '0.00',
        r.status || '—'
      ]);
      autoTable(doc, {
        startY: y,
        head: [['Recharge ID', 'Mobile', 'Operator', 'Plan', 'Amount (INR)', 'Status']],
        body: rechargeData,
        theme: 'striped',
        headStyles: { fillColor: [99, 102, 241], fontSize: 8 },
        styles: { fontSize: 7 },
        margin: { left: 15, right: 15 }
      });
    } catch (e) {
      doc.text('Recharge data unavailable.', 15, y);
    }

    this.addPdfFooter(doc);
  }

  async downloadPdf(): Promise<void> {
    const analytics = await firstValueFrom(
      this.dashboardState.getAnalytics().pipe(filter((a): a is DashboardAnalytics => a !== null))
    );
    if (!analytics) throw new Error('No analytics data available.');

    const doc = new jsPDF('p', 'mm', 'a4');
    await this.generateReportContent(doc, analytics);
    
    const dateStr = new Date().toISOString().split('T')[0];
    doc.save(`OmniCharge_Executive_Report_${dateStr}.pdf`);
  }

  private addPdfFooter(doc: any): void {
    const pageCount = doc.internal.getNumberOfPages();
    for (let i = 1; i <= pageCount; i++) {
      doc.setPage(i);
      doc.setFontSize(7);
      doc.setTextColor(148, 163, 184);
      const pageH = doc.internal.pageSize.getHeight();
      doc.text(`Page ${i} of ${pageCount}`, doc.internal.pageSize.getWidth() / 2, pageH - 8, { align: 'center' });
      doc.text('© 2026 OmniCharge. All rights reserved. CONFIDENTIAL.', 15, pageH - 8);
    }
  }

  private buildEmailReportHtml(analytics: DashboardAnalytics): string {
    const { payments, recharges, users, plans } = analytics;
    const now = new Date().toLocaleString('en-IN', { dateStyle: 'full', timeStyle: 'short' });
    const fmt = (v: number) => `₹${v.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    const growthArrow = payments.revenueGrowthPercentage >= 0 ? '📈' : '📉';
    const growthColor = payments.revenueGrowthPercentage >= 0 ? '#059669' : '#dc2626';

    const topPlansHtml = (recharges.topPlans || []).slice(0, 5).map((p, i) =>
      `<tr><td style="padding:8px 12px;border-bottom:1px solid #e2e8f0;font-weight:bold;">${i + 1}. ${p.planName}</td><td style="padding:8px 12px;border-bottom:1px solid #e2e8f0;">${p.operatorName}</td><td style="padding:8px 12px;border-bottom:1px solid #e2e8f0;font-weight:bold;">${p.rechargeCount}</td><td style="padding:8px 12px;border-bottom:1px solid #e2e8f0;">${fmt(p.totalRevenue)}</td></tr>`
    ).join('');

    const topSpendersHtml = (payments.topSpenders || []).slice(0, 5).map((s, i) =>
      `<tr><td style="padding:8px 12px;border-bottom:1px solid #e2e8f0;font-weight:bold;">${i + 1}. ${s.fullName || s.userEmail || 'Unknown'}</td><td style="padding:8px 12px;border-bottom:1px solid #e2e8f0;">${s.transactionCount} txns</td><td style="padding:8px 12px;border-bottom:1px solid #e2e8f0;font-weight:bold;">${fmt(s.totalSpent)}</td></tr>`
    ).join('');

    return `<!DOCTYPE html><html><head><style>
      body{font-family:Arial,sans-serif;color:#1e293b;line-height:1.6;margin:0;padding:0}
      .header{background:linear-gradient(135deg,#4f46e5 0%,#7c3aed 100%);color:white;padding:30px;text-align:center}
      .content{padding:30px;background:#f8fafc}
      .card{background:white;border-radius:12px;padding:20px;margin-bottom:16px;border:1px solid #e2e8f0}
      .grid{display:flex;gap:16px;flex-wrap:wrap}
      .metric{flex:1;min-width:140px}
      .metric-value{font-size:24px;font-weight:900;color:#0f172a}
      .metric-label{font-size:11px;text-transform:uppercase;letter-spacing:1px;color:#64748b;font-weight:700}
      table{width:100%;border-collapse:collapse;font-size:13px}
      th{background:#f1f5f9;padding:10px 12px;text-align:left;font-size:11px;text-transform:uppercase;letter-spacing:0.5px;color:#475569;font-weight:700}
      .footer{text-align:center;padding:20px;color:#94a3b8;font-size:12px}
    </style></head><body>
    <div class="header">
      <h1 style="margin:0;font-size:28px">OmniCharge Executive Summary</h1>
      <p style="margin:5px 0 0;opacity:0.9;font-size:14px">${now}</p>
    </div>
    <div class="content">
      <div class="card"><h2 style="margin:0 0 16px;font-size:16px;color:#0f172a">Key Metrics</h2>
        <div class="grid">
          <div class="metric"><div class="metric-label">Gross Revenue</div><div class="metric-value">${fmt(payments.grossRevenue)}</div></div>
          <div class="metric"><div class="metric-label">Today Revenue</div><div class="metric-value">${fmt(payments.todayRevenue)}</div></div>
          <div class="metric"><div class="metric-label">Total Recharges</div><div class="metric-value">${recharges.totalRecharges.toLocaleString()}</div></div>
          <div class="metric"><div class="metric-label">Success Rate</div><div class="metric-value">${recharges.successRate.toFixed(1)}%</div></div>
        </div>
      </div>
      <div class="card"><h2 style="margin:0 0 8px;font-size:16px;color:#0f172a">Revenue Growth</h2>
        <p style="margin:0">Month Revenue: <strong>${fmt(payments.monthRevenue)}</strong> | Last Month: <strong>${fmt(payments.lastMonthRevenue)}</strong></p>
        <p style="margin:4px 0 0;font-size:18px;font-weight:900;color:${growthColor}">${growthArrow} ${payments.revenueGrowthPercentage >= 0 ? '+' : ''}${payments.revenueGrowthPercentage.toFixed(1)}% MoM</p>
      </div>
      <div class="card"><h2 style="margin:0 0 8px;font-size:16px;color:#0f172a">Users & Plans</h2>
        <div class="grid">
          <div class="metric"><div class="metric-label">Total Users</div><div class="metric-value">${users.totalUsers.toLocaleString()}</div></div>
          <div class="metric"><div class="metric-label">New This Month</div><div class="metric-value">${users.newUsersThisMonth}</div></div>
          <div class="metric"><div class="metric-label">Active Plans</div><div class="metric-value">${plans.activePlans}</div></div>
          <div class="metric"><div class="metric-label">Total Plans</div><div class="metric-value">${plans.totalPlans}</div></div>
        </div>
      </div>
      <div class="card"><h2 style="margin:0 0 12px;font-size:16px;color:#0f172a">Top 5 Plans</h2>
        <table><tr><th>Plan</th><th>Operator</th><th>Volume</th><th>Revenue</th></tr>${topPlansHtml}</table>
      </div>
      <div class="card"><h2 style="margin:0 0 12px;font-size:16px;color:#0f172a">Top 5 Spenders</h2>
        <table><tr><th>User</th><th>Transactions</th><th>Total Spent</th></tr>${topSpendersHtml}</table>
      </div>
    </div>
    <div class="footer">
      <p>This report was auto-generated by OmniCharge Admin Console.</p>
      <p>© 2026 OmniCharge. All rights reserved. CONFIDENTIAL.</p>
    </div>
    </body></html>`;
  }
}
