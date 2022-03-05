package com.tangem.tap.features.details.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import by.kirich1409.viewbindingdelegate.viewBinding
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
import com.tangem.wallet.databinding.FragmentDetailsBinding
import org.rekotlin.StoreSubscriber

class DetailsFragment : Fragment(R.layout.fragment_details), StoreSubscriber<DetailsState> {

    private var currencySelectionDialog = CurrencySelectionDialog()
    private val binding: FragmentDetailsBinding by viewBinding(FragmentDetailsBinding::bind)

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

        binding.toolbar.setNavigationOnClickListener {
            store.dispatch(NavigationAction.PopBackTo())
        }
    }


    override fun newState(state: DetailsState) {
        if (activity == null || view == null) return
        setState(state)
    }

    private fun setState(state: DetailsState)  = with (binding){

        if (state.cardInfo != null) {
            val cardId = if (state.isTangemTwins) {
                state.scanResponse?.card?.getTwinCardIdForUser()
            } else {
                state.cardInfo.cardId
            }
            tvCardId.text = cardId
            tvIssuer.text = state.cardInfo.issuer
            tvSignedHashes.text = getString(
                R.string.details_row_subtitle_signed_hashes_format,
                state.cardInfo.signedHashes.toString()
            )
        }

        tvSignedHashes.show(!state.isTangemTwins)
        tvSignedHashesTitle.show(!state.isTangemTwins)

        tvDisclaimer.setOnClickListener { store.dispatch(DetailsAction.ShowDisclaimer) }

        tvCardTou.show(state.cardTermsOfUseUrl != null)
        tvCardTou.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = state.cardTermsOfUseUrl
            startActivity(intent)
        }

        if (state.isTangemTwins) {
            tvResetToFactory.show()
            tvResetToFactory.text = getText(R.string.details_row_title_twins_recreate)
            tvResetToFactory.setOnClickListener {
                store.dispatch(DetailsAction.ReCreateTwinsWallet(state.twinCardsState.cardNumber!!))
            }
        } else {
            tvResetToFactory.show()
            tvResetToFactory.text = getText(R.string.details_row_title_reset_factory_settings)
            tvResetToFactory.setOnClickListener {
                store.dispatch(DetailsAction.ResetToFactory.Check)
                store.dispatch(DetailsAction.ResetToFactory.Proceed)
            }
        }

        tvCreateBackup.show(state.createBackupAllowed)
        tvCreateBackup.setOnClickListener {
            store.dispatch(DetailsAction.CreateBackup)
        }

        tvAppCurrency.text = state.appCurrencyState.fiatCurrencyName

        tvAppCurrencyTitle.setOnClickListener {
            store.dispatch(DetailsAction.AppCurrencyAction.ChooseAppCurrency)
        }
        tvSendFeedback.setOnClickListener {
            store.dispatch(GlobalAction.SendFeedback(FeedbackEmail()))
        }

        tvWalletConnect.show(state.scanResponse?.card?.isMultiwalletAllowed == true)
        tvWalletConnect.setOnClickListener {
            store.dispatch(NavigationAction.NavigateTo(AppScreen.WalletConnectSessions))
        }

        llManageSecurity.setOnClickListener {
            store.dispatch(DetailsAction.ManageSecurity.CheckCurrentSecurityOption(state.scanResponse!!.card))
        }

        val currentSecurity = when (state.securityScreenState?.currentOption) {
            SecurityOption.LongTap -> R.string.details_manage_security_long_tap
            SecurityOption.PassCode -> R.string.details_manage_security_passcode
            SecurityOption.AccessCode -> R.string.details_manage_security_access_code
            null -> null
        }
        currentSecurity?.let { tvSecurity.text = getString(it) }

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