package com.tangem.tap.features.onboarding.products.otherCards

import android.os.Bundle
import android.view.View
import androidx.transition.TransitionInflater
import com.tangem.tap.features.send.BaseStoreFragment
import com.tangem.wallet.R
import org.rekotlin.StoreSubscriber

/**
[REDACTED_AUTHOR]
 */
class OnboardingOtherCardsFragment : BaseStoreFragment(R.layout.fragment_onboarding_main), StoreSubscriber<OnboardingOtherCardsState> {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

class OnboardingOtherCardsState {}