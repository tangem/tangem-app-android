package com.tangem.tap.features.wallet.ui.dialogs

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_twin_cards.*

class TwinsOnboardingFragment : Fragment(R.layout.fragment_twin_cards) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
            }
        })
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_right)
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        store.dispatch(WalletAction.TwinsAction.SetOnboardingShown)

        val secondCardId = store.state.walletState.twinCardsState?.secondCardId
        val text = getString(R.string.twins_onboarding_description_format, secondCardId)
        tv_twin_cards_description_1.text = text

        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        btn_continue.setOnClickListener {
            store.dispatch(NavigationAction.NavigateTo(AppScreen.Wallet))
        }
    }

}
