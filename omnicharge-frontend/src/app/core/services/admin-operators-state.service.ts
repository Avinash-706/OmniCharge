import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { shareReplay, tap, catchError, map } from 'rxjs/operators';
import { AdminOperatorService, AdminOperatorResponse, OperatorRequest } from './admin-operator.service';
import { AdminPlanService, AdminPlanResponse, PlanRequest } from './admin-plan.service';
import { AdminRechargeService, OperatorPlansResponse } from './admin-recharge.service';

@Injectable({
  providedIn: 'root'
})
export class AdminOperatorStateService {
  private operatorService = inject(AdminOperatorService);
  private planService = inject(AdminPlanService);
  private rechargeService = inject(AdminRechargeService);

  // Core State Managers
  private operatorsSubj$ = new BehaviorSubject<AdminOperatorResponse[] | null>(null);
  
  // Cache dictionaries
  private selectedPlansSubj$ = new BehaviorSubject<Record<number, AdminPlanResponse[]>>({}); 
  private operatorStatsSubj$ = new BehaviorSubject<Record<number, OperatorPlansResponse>>({});

  private loadingSubj$ = new BehaviorSubject<boolean>(false);
  private errorSubj$ = new BehaviorSubject<string | null>(null);

  // Exposed Observables
  public readonly operators$: Observable<AdminOperatorResponse[] | null> = this.operatorsSubj$.asObservable().pipe(shareReplay(1));
  public readonly loading$: Observable<boolean> = this.loadingSubj$.asObservable();
  public readonly error$: Observable<string | null> = this.errorSubj$.asObservable();

  /**
   * Fetch all operators into cache
   */
  loadOperators(force: boolean = false): void {
    if (this.operatorsSubj$.value !== null && !force) return;

    this.loadingSubj$.next(true);
    this.operatorService.getAllOperators().pipe(
      tap(data => {
        this.operatorsSubj$.next(data);
        this.loadingSubj$.next(false);
      }),
      catchError(err => {
        this.errorSubj$.next('Failed to retrieve Operator core data.');
        this.loadingSubj$.next(false);
        return of(null);
      })
    ).subscribe();
  }

  /**
   * Fetch plans for an operator.
   */
  getOperatorPlans(operatorId: number, force: boolean = false): Observable<AdminPlanResponse[]> {
    const cached = this.selectedPlansSubj$.value[operatorId];
    if (cached && !force) {
      return of(cached);
    }
    
    return new Observable<AdminPlanResponse[]>(obs => {
      this.loadingSubj$.next(true);
      this.planService.getOperatorPlans(operatorId).subscribe({
        next: (res) => {
          const dict = { ...this.selectedPlansSubj$.value };
          dict[operatorId] = res.data;
          this.selectedPlansSubj$.next(dict);
          this.loadingSubj$.next(false);
          obs.next(res.data);
          obs.complete();
        },
        error: () => {
          this.loadingSubj$.next(false);
          obs.next([]);
          obs.complete();
        }
      });
    });
  }

  /**
   * Fetch performance stats for an operator from Recharge Service (BI Hub)
   */
  getOperatorStats(operatorId: number, force: boolean = false): Observable<OperatorPlansResponse | null> {
    const cached = this.operatorStatsSubj$.value[operatorId];
    if (cached && !force) {
      return of(cached);
    }

    return new Observable<OperatorPlansResponse | null>(obs => {
      this.rechargeService.getOperatorPlans(operatorId).subscribe({
        next: (res) => {
          const dict = { ...this.operatorStatsSubj$.value };
          dict[operatorId] = res;
          this.operatorStatsSubj$.next(dict);
          obs.next(res);
          obs.complete();
        },
        error: () => {
          obs.next(null);
          obs.complete();
        }
      });
    });
  }

  // Allow observing the direct dict values for pure localized reactivity 
  observeOperatorPlans(operatorId: number): Observable<AdminPlanResponse[]> {
    return new Observable<AdminPlanResponse[]>(obs => {
        this.selectedPlansSubj$.subscribe(dict => {
            obs.next(dict[operatorId] || []);
        });
    });
  }

  observeOperatorStats(operatorId: number): Observable<OperatorPlansResponse | null> {
    return new Observable<OperatorPlansResponse | null>(obs => {
        this.operatorStatsSubj$.subscribe(dict => {
            obs.next(dict[operatorId] || null);
        });
    });
  }

  // --- CRUD OPERATOR ---

  createOperator(req: OperatorRequest): Observable<boolean> {
    return this.operatorService.createOperator(req).pipe(
      map(res => {
         this.loadOperators(true); // Cache Purge
         return true;
      }),
      catchError(() => of(false))
    );
  }

  updateOperator(id: number, req: OperatorRequest): Observable<boolean> {
    return this.operatorService.updateOperator(id, req).pipe(
      map(res => {
         this.loadOperators(true); // Cache Purge
         return true;
      }),
      catchError(() => of(false))
    );
  }

  deleteOperator(id: number): Observable<boolean> {
    return this.operatorService.deleteOperator(id).pipe(
        map(() => {
            this.loadOperators(true); // Purge cache
            return true;
        }),
        catchError(() => of(false))
    );
  }

  // --- CRUD PLAN ---

  createPlan(operatorId: number, req: PlanRequest): Observable<boolean> {
    return this.planService.createPlan(operatorId, req).pipe(
        map(() => {
            this.getOperatorPlans(operatorId, true); // Evict Cache
            this.getOperatorStats(operatorId, true); // Evict Stats Cache
            return true;
        }),
        catchError(() => of(false))
    );
  }

  updatePlan(operatorId: number, planId: number, req: PlanRequest): Observable<boolean> {
    return this.planService.updatePlan(planId, req).pipe(
        map(() => {
            this.getOperatorPlans(operatorId, true);
            this.getOperatorStats(operatorId, true);
            return true;
        }),
        catchError(() => of(false))
    );
  }

  // --- OPTIMISTIC UPDATES ---

  toggleOperatorStatus(operator: AdminOperatorResponse): Observable<boolean> {
    const currentOps = this.operatorsSubj$.value;
    if (!currentOps) return of(false);
    
    const idx = currentOps.findIndex(o => o.id === operator.id);
    if (idx === -1) return of(false);
    
    const originalStatus = operator.isActive;
    const targetStatus = !originalStatus;

    const ops = [...currentOps];
    ops[idx] = { ...operator, isActive: targetStatus };
    this.operatorsSubj$.next(ops);

    const apiCall = targetStatus 
        ? this.operatorService.activateOperator(operator.id) 
        : this.operatorService.deactivateOperator(operator.id);

    return new Observable<boolean>(obs => {
      apiCall.subscribe({
        next: () => { obs.next(true); obs.complete(); },
        error: () => {
          const rollback = [...this.operatorsSubj$.value!];
          const rollbackIdx = rollback.findIndex(o => o.id === operator.id);
          if (rollbackIdx !== -1) {
            rollback[rollbackIdx] = { ...rollback[rollbackIdx], isActive: originalStatus };
            this.operatorsSubj$.next(rollback);
          }
          obs.next(false); obs.complete();
        }
      });
    });
  }

  togglePlanStatus(operatorId: number, plan: AdminPlanResponse): Observable<boolean> {
    const currentDict = this.selectedPlansSubj$.value;
    const plans = currentDict[operatorId];
    if (!plans) return of(false);
    
    const idx = plans.findIndex(p => p.id === plan.id);
    if (idx === -1) return of(false);
    
    const originalStatus = plan.isActive;
    const targetStatus = !originalStatus;

    const mutatedPlans = [...plans];
    mutatedPlans[idx] = { ...plan, isActive: targetStatus };
    const nextDict = { ...currentDict, [operatorId]: mutatedPlans };
    this.selectedPlansSubj$.next(nextDict);

    const apiCall = targetStatus 
        ? this.planService.activatePlan(plan.id) 
        : this.planService.deactivatePlan(plan.id);

    return new Observable<boolean>(obs => {
       apiCall.subscribe({
        next: () => { obs.next(true); obs.complete(); },
        error: () => {
          const rDict = { ...this.selectedPlansSubj$.value };
          const rPlans = [...(rDict[operatorId] || [])];
          const rIdx = rPlans.findIndex(p => p.id === plan.id);
          if (rIdx !== -1) {
            rPlans[rIdx] = { ...rPlans[rIdx], isActive: originalStatus };
            rDict[operatorId] = rPlans;
            this.selectedPlansSubj$.next(rDict);
          }
          obs.next(false); obs.complete();
        }
      });
    });
  }
}
