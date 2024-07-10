package com.tangem.tap.features.onboarding.products

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import by.kirich1409.viewbindingdelegate.viewBinding
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.transitions.HomeToOnboardingTransition
import com.tangem.tap.features.BaseStoreFragment
import com.tangem.tap.features.onboarding.OnboardingMenuProvider
import com.tangem.tap.store
import com.tangem.utils.Provider
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
    }

    override fun loadToolbarMenu(): MenuProvider = OnboardingMenuProvider(
        scanResponseProvider = Provider {
            store.state.globalState.onboardingState.onboardingManager?.scanResponse
                ?: store.state.detailsState.scanResponse
                ?: error("ScanResponse must be not null")
        },
    )

    protected fun showConfetti(show: Boolean) = with(binding.vConfetti) {
        lavConfetti.show(show)

        if (show) {
            lavConfetti.playAnimation()
        } else {
            lavConfetti.cancelAnimation()
        }
    }
}