package com.tangem.tap.features.onboarding.products

import android.os.Bundle
import android.view.View
import androidx.transition.TransitionInflater
import com.tangem.tap.common.extensions.inflate
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.common.transitions.HomeToOnboardingTransition
import com.tangem.tap.features.send.BaseStoreFragment
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_onboarding_main.*
import kotlinx.android.synthetic.main.layout_pseudo_toolbar.*
import kotlinx.android.synthetic.main.view_confetti.*
import org.rekotlin.StoreSubscriber

/**
[REDACTED_AUTHOR]
 */
abstract class BaseOnboardingFragment<T> : BaseStoreFragment(R.layout.fragment_onboarding_main), StoreSubscriber<T> {

    protected var fromScreen: AppScreen? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        fromScreen = store.state.globalState.onboardingService?.fromScreen

        super.onCreate(savedInstanceState)
    }

    override fun configureTransitions() {
        val inflater = TransitionInflater.from(requireContext())
        when (fromScreen) {
            AppScreen.Home -> {
                enterTransition = HomeToOnboardingTransition()
                exitTransition = HomeToOnboardingTransition()
            }
            AppScreen.Wallet -> {
                enterTransition = inflater.inflateTransition(android.R.transition.slide_bottom)
                exitTransition = inflater.inflateTransition(android.R.transition.slide_top)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onboarding_top_container.inflate(getOnboardingTopContainerId(), true)
        configureToolbar()
    }

    private fun configureToolbar() {
        when (fromScreen) {
            AppScreen.Home -> {
                toolbar.show(true)
                pseudo_toolbar.visibility = View.GONE
            }
            AppScreen.Wallet -> {
                toolbar.visibility = View.GONE
                pseudo_toolbar.show(true)
                imv_close.setOnClickListener {
                    store.dispatch(NavigationAction.PopBackTo())
                }
            }
        }
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