package com.tangem.tap.features.details.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.isMultiwalletAllowed
import com.tangem.tap.domain.twins.getTwinCardIdForUser
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.features.details.redux.SecurityOption
import com.tangem.tap.features.feedback.FeedbackEmail
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
            val cardId = if (state.isTangemTwins) {
                state.scanResponse?.card?.getTwinCardIdForUser()
            } else {
                state.cardInfo.cardId
            }
            tv_card_id.text = cardId
            tv_issuer.text = state.cardInfo.issuer
            tv_signed_hashes.text = state.cardInfo.signedHashes.toString()
        }

        tv_signed_hashes.show(!state.isTangemTwins)
        tv_signed_hashes_title.show(!state.isTangemTwins)

        tv_disclaimer.setOnClickListener { store.dispatch(DetailsAction.ShowDisclaimer) }

        tv_card_tou.show(state.cardTermsOfUseUrl != null)
        tv_card_tou.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = state.cardTermsOfUseUrl
            startActivity(intent)
        }

        if (state.isTangemTwins) {
            tv_erase_wallet.show()
            tv_erase_wallet.text = getText(R.string.details_row_title_twins_recreate)
            tv_erase_wallet.setOnClickListener {
                store.dispatch(DetailsAction.ReCreateTwinsWallet(state.twinCardsState.cardNumber!!))
            }
        } else {
            tv_erase_wallet.show()
            tv_erase_wallet.text = getText(R.string.details_row_title_erase_wallet)
            tv_erase_wallet.setOnClickListener {
                store.dispatch(DetailsAction.EraseWallet.Check)
                store.dispatch(DetailsAction.EraseWallet.Proceed)
            }
        }


        tv_app_currency.text = state.appCurrencyState.fiatCurrencyName

        tv_app_currency_title.setOnClickListener {
            store.dispatch(DetailsAction.AppCurrencyAction.ChooseAppCurrency)
        }
        tv_send_feedback.setOnClickListener {
            store.dispatch(GlobalAction.SendFeedback(FeedbackEmail()))
        }

        tv_wallet_connect.show(state.scanResponse?.card?.isMultiwalletAllowed == true)
        tv_wallet_connect.setOnClickListener {
            store.dispatch(NavigationAction.NavigateTo(AppScreen.WalletConnectSessions))
        }

        tv_security_title.setOnClickListener {
            store.dispatch(DetailsAction.ManageSecurity.CheckCurrentSecurityOption(
                state.scanResponse?.card?.cardId
            ))
        }

        val currentSecurity = when (state.securityScreenState?.currentOption) {
            SecurityOption.LongTap -> R.string.details_manage_security_long_tap
            SecurityOption.PassCode -> R.string.details_manage_security_passcode
            SecurityOption.AccessCode -> R.string.details_manage_security_access_code
            null -> null
        }
        currentSecurity?.let { tv_security.text = getString(it) }

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