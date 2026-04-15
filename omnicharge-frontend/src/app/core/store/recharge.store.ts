import { signalStore, withState, withMethods, withHooks, patchState } from '@ngrx/signals';

/**
 * Matches backend: OperatorDetectionResponse.java
 * Fields: operatorId, operatorName, operatorCode, logoUrl, plans[]
 */
export interface OperatorData {
  operatorId: number;
  operatorName: string;
  operatorCode: string;
  logoUrl: string | null;
}

/**
 * Matches backend: PlanResponse.java (operator-service)
 * Fields: id, operatorId, operatorName, planName, price, validityDays,
 *         dataLimit, callBenefit, smsBenefit, additionalBenefits, category, isActive
 */
export interface PlanData {
  id: number;
  operatorId: number;
  operatorName: string;
  planName: string;
  price: number;
  validityDays: number;
  dataLimit: string;
  callBenefit: string;
  smsBenefit: string;
  additionalBenefits: string | null;
  category: 'RECOMMENDED' | 'DATA' | 'UNLIMITED' | 'TALKTIME';
  isActive: boolean;
}

interface RechargeState {
  targetMobileNumber: string | null;
  detectedOperator: OperatorData | null;
  selectedPlan: PlanData | null;
}

const initialState: RechargeState = {
  targetMobileNumber: null,
  detectedOperator: null,
  selectedPlan: null
};

const STORAGE_KEY = 'omnicharge_recharge_flow';

export const RechargeFlowStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withMethods((store) => {
    const saveToSession = (state: RechargeState) => {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    };

    return {
      setMobileNumber(mobileNumber: string) {
        patchState(store, { targetMobileNumber: mobileNumber });
        saveToSession({ targetMobileNumber: mobileNumber, detectedOperator: store.detectedOperator(), selectedPlan: store.selectedPlan() });
      },
      setOperator(operator: OperatorData) {
        patchState(store, { detectedOperator: operator });
        saveToSession({ targetMobileNumber: store.targetMobileNumber(), detectedOperator: operator, selectedPlan: store.selectedPlan() });
      },
      selectPlan(plan: PlanData) {
        patchState(store, { selectedPlan: plan });
        saveToSession({ targetMobileNumber: store.targetMobileNumber(), detectedOperator: store.detectedOperator(), selectedPlan: plan });
      },
      clearFlow() {
        patchState(store, initialState);
        sessionStorage.removeItem(STORAGE_KEY);
      }
    };
  }),
  withHooks({
    onInit(store) {
      const stored = sessionStorage.getItem(STORAGE_KEY);
      if (stored) {
        try {
          const parsed = JSON.parse(stored);
          patchState(store, parsed);
        } catch (e) {
          sessionStorage.removeItem(STORAGE_KEY);
        }
      }
    }
  })
);
