package com.tangem.tap.features.details.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.google.accompanist.appcompattheme.AppCompatTheme
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.store
import com.tangem.wallet.R
import org.rekotlin.StoreSubscriber

class DetailsFragment : Fragment(), StoreSubscriber<DetailsState> {

    private val detailsViewModel = DetailsViewModel(store)

    private var detailsScreenState: MutableState<DetailsScreenState> =
        mutableStateOf(detailsViewModel.updateState(store.state.detailsState))


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_right)
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                isTransitionGroup = true
                AppCompatTheme {
                    DetailsScreen(
                        state = detailsScreenState.value,
                        onBackPressed = { store.dispatch(NavigationAction.PopBackTo()) },
                    )
                }
            }
        }
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

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        binding.toolbar.setNavigationOnClickListener {
//            store.dispatch(NavigationAction.PopBackTo())
//        }
//    }

    override fun newState(state: DetailsState) {
        if (activity == null || view == null) return
        detailsScreenState.value = detailsViewModel.updateState(state)
    }

//    private fun setState(state: DetailsState) = with(binding) {
//
//        if (state.cardInfo != null) {
//            val cardId = if (state.isTangemTwins) {
//                state.scanResponse?.card?.getTwinCardIdForUser()
//            } else {
//                state.cardInfo.cardId
//            }
//            tvCardId.text = cardId
//            tvIssuer.text = state.cardInfo.issuer
//            tvSignedHashes.text = getString(
//                R.string.details_row_subtitle_signed_hashes_format,
//                state.cardInfo.signedHashes.toString()
//            )
//        }
//
//        tvSignedHashes.show(!state.isTangemTwins)
//        tvSignedHashesTitle.show(!state.isTangemTwins)
//
//        tvDisclaimer.setOnClickListener { store.dispatch(DetailsAction.ShowDisclaimer) }
//
//
//        if (state.isTangemTwins) {
//            tvResetToFactory.show()
//            tvResetToFactory.text = getText(R.string.details_row_title_twins_recreate)
//            tvResetToFactory.setOnClickListener {
//                store.dispatch(DetailsAction.ReCreateTwinsWallet(state.twinCardsState.cardNumber!!))
//            }
//        } else {
//            tvResetToFactory.show()
//            tvResetToFactory.text = getText(R.string.details_row_title_reset_factory_settings)
//            tvResetToFactory.setOnClickListener {
//                store.dispatch(DetailsAction.ResetToFactory.Check)
//                store.dispatch(DetailsAction.ResetToFactory.Proceed)
//            }
//        }
//
//
//        tvAppCurrency.show(state.scanResponse?.card?.isMultiwalletAllowed != true)
//        tvAppCurrencyTitle.show(state.scanResponse?.card?.isMultiwalletAllowed != true)
//        tvAppCurrency.text = state.appCurrency.code
//
//        tvAppCurrency.setOnClickListener {
//            store.dispatch(WalletAction.AppCurrencyAction.ChooseAppCurrency)
//        }
//
//        tvSecurityAndPrivacy.setOnClickListener {
//            store.dispatch(DetailsAction.ManageSecurity.CheckCurrentSecurityOption(state.scanResponse!!.card))
//            tvSecurityAndPrivacy.setOnClickListener {
//                store.dispatch(NavigationAction.NavigateTo(AppScreen.SecurityAndPrivacy))
//            }
//
//        }
//
//    }

}