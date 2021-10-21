package com.tangem.tap.features.onboarding.products.twins

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.squareup.picasso.Picasso
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.withMainContext
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.twins.TwinsHelper
import com.tangem.tap.features.wallet.redux.Artwork
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_twin_cards_welcome.*
import kotlinx.android.synthetic.main.layout_twin_cards.*
import kotlinx.coroutines.launch

class OnboardingTwinsFragment : Fragment(R.layout.fragment_twin_cards_welcome) {

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

        val secondCardId = store.state.walletState.twinCardsState?.secondCardId ?: ""
        val secondTwinCardId = TwinsHelper.getTwinCardIdForUser(secondCardId)
        val text = getString(R.string.twins_onboarding_description_format, secondTwinCardId)
        tv_twin_cards_description_1.text = text

        setOnClickListeners()

        Picasso.get()
                .load(Artwork.TWIN_CARD_1)
                .placeholder(R.drawable.card_placeholder)
                ?.error(R.drawable.card_placeholder)
                ?.into(iv_twin_card_1)

        Picasso.get()
                .load(Artwork.TWIN_CARD_2)
                .placeholder(R.drawable.card_placeholder)
                ?.error(R.drawable.card_placeholder)
                ?.into(iv_twin_card_2)
    }

    private fun setOnClickListeners() {
        btn_continue.setOnClickListener {
            scope.launch {
                val global = store.state.globalState
                val scanResponse = global.onboardingManager?.scanResponse ?: return@launch
                global.tapWalletManager.onCardScanned(scanResponse)
                withMainContext {
                    store.dispatchDebugErrorNotification("Делаем онбоардинг")
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.Wallet))
                }
            }
        }
    }
}