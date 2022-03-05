package com.tangem.tap.features.onboarding.products

import by.kirich1409.viewbindingdelegate.viewBinding
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.transitions.HomeToOnboardingTransition
import com.tangem.tap.features.BaseStoreFragment
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentOnboardingMainBinding
import com.tangem.wallet.databinding.ViewOnboardingProgressBinding
import org.rekotlin.StoreSubscriber

/**
[REDACTED_AUTHOR]
 */
abstract class BaseOnboardingFragment<T> : BaseStoreFragment(R.layout.fragment_onboarding_main), StoreSubscriber<T> {

    protected val binding: FragmentOnboardingMainBinding by viewBinding(FragmentOnboardingMainBinding::bind)
    protected val pbBinding: ViewOnboardingProgressBinding by viewBinding(ViewOnboardingProgressBinding::bind)

    override fun configureTransitions() {
        enterTransition = HomeToOnboardingTransition()
        exitTransition = HomeToOnboardingTransition()
    }


    protected fun showConfetti(show: Boolean) = with(binding.vConfetti) {
        lavConfetti.show(show)

        if (show) {
            lavConfetti.playAnimation()
        } else {
            lavConfetti.cancelAnimation()
        }
    }
}