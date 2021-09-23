package com.tangem.tap.features.onboarding.products.old

import android.os.Bundle
import android.view.View
import androidx.transition.TransitionInflater
import com.tangem.tap.common.extensions.addOnBackPressedDispatcher
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.onboarding.redux.OnboardingAction
import com.tangem.tap.features.onboarding.redux.OnboardingOtherCardsState
import com.tangem.tap.features.onboarding.redux.OnboardingOtherStep
import com.tangem.tap.features.send.BaseStoreFragment
import com.tangem.tap.store
import com.tangem.wallet.R
import org.rekotlin.StoreSubscriber

/**
[REDACTED_AUTHOR]
 */
class OnboardingOtherCardsFragment : BaseStoreFragment(R.layout.fragment_onboarding), StoreSubscriber<OnboardingOtherCardsState> {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.addOnBackPressedDispatcher {
            store.dispatch(NavigationAction.PopBackTo())
            store.dispatch(OnboardingAction.SetInitialStepOfScreen(OnboardingOtherStep.Done))
        }

        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.fade)
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun subscribeToStore() {
//        store.subscribe(this) { state ->
//            state.skipRepeats { oldState, newState ->
//                oldState.onboardingOtherState == newState.onboardingOtherState
//            }.select { it.onboardingOtherState }
//        }
    }

    override fun newState(state: OnboardingOtherCardsState) {
        if (activity == null) return

    }
}