package com.tangem.tap.features.onboarding.products

import android.os.Bundle
import android.view.View
import com.tangem.tap.common.extensions.inflate
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.transitions.HomeToOnboardingTransition
import com.tangem.tap.features.BaseStoreFragment
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_onboarding_main.*
import kotlinx.android.synthetic.main.view_confetti.*
import org.rekotlin.StoreSubscriber

/**
[REDACTED_AUTHOR]
 */
abstract class BaseOnboardingFragment<T> : BaseStoreFragment(R.layout.fragment_onboarding_main), StoreSubscriber<T> {

    override fun configureTransitions() {
        enterTransition = HomeToOnboardingTransition()
        exitTransition = HomeToOnboardingTransition()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onboarding_top_container.inflate(getOnboardingTopContainerId(), true)
    }

    abstract fun getOnboardingTopContainerId(): Int

    protected fun showConfetti(show: Boolean) {
        lav_confetti.show(show)

        if (show) {
            lav_confetti.playAnimation()
        } else {
            lav_confetti.cancelAnimation()
        }
    }
}