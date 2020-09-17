package com.tangem.tap.features.details.ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_details.*
import kotlinx.android.synthetic.main.fragment_wallet.toolbar
import org.rekotlin.StoreSubscriber

class DetailsFragment : Fragment(R.layout.fragment_details), StoreSubscriber<DetailsState> {

    private var currencySelectionDialog = CurrencySelectionDialog()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(NavigationAction.PopBackTo())
            }
        })
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_right)
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun onStart() {
        super.onStart()
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.detailsState == newState.detailsState
            }.select { it.detailsState }
        }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener {
            store.dispatch(NavigationAction.PopBackTo())
        }
    }


    override fun newState(state: DetailsState) {
        if (activity == null) return


        if (state.cardInfo != null) {
            tv_card_id.text = state.cardInfo.cardId
            tv_issuer.text = state.cardInfo.issuer
            tv_signed_hashes.text = state.cardInfo.signedHashes.toString()
        }

        tv_erase_wallet.setOnClickListener {
            store.dispatch(DetailsAction.EraseWallet.Check)
            store.dispatch(DetailsAction.EraseWallet.Proceed)
        }

        tv_app_currency.text = state.appCurrencyState.fiatCurrencyName

        tv_app_currency_title.setOnClickListener {
            store.dispatch(DetailsAction.AppCurrencyAction.ChooseAppCurrency)
        }

        if (state.appCurrencyState.showAppCurrencyDialog &&
                !state.appCurrencyState.fiatCurrencies.isNullOrEmpty()) {
            currencySelectionDialog.show(
                    state.appCurrencyState.fiatCurrencies,
                    state.appCurrencyState.fiatCurrencyName,
                    requireContext()
            )
        } else {
            currencySelectionDialog.clear()
        }

    }

}
