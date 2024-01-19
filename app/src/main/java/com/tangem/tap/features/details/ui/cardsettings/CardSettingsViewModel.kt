package com.tangem.tap.features.details.ui.cardsettings

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import arrow.core.Either
import com.tangem.core.analytics.Analytics
import com.tangem.domain.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.common.getTwinCardIdForUser
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.features.details.redux.CardSettingsState
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.store
import dagger.hilt.android.lifecycle.HiltViewModel
import org.rekotlin.StoreSubscriber
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class CardSettingsViewModel @Inject constructor(
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
) :
    ViewModel(), DefaultLifecycleObserver, StoreSubscriber<DetailsState> {

    var screenState: MutableState<CardSettingsScreenState> =
        mutableStateOf(updateState(store.state.detailsState.cardSettingsState))

    override fun onStart(owner: LifecycleOwner) {
        when (val selectedWalletEither = getSelectedWalletSyncUseCase()) {
            is Either.Left -> {
                Timber.e(selectedWalletEither.value.toString())
            }
            is Either.Right -> Unit
        }

        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.detailsState == newState.detailsState
            }.select { it.detailsState }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        store.unsubscribe(this)
    }

    override fun newState(state: DetailsState) {
        screenState.value = updateState(state.cardSettingsState)
    }

    private fun updateState(state: CardSettingsState?): CardSettingsScreenState {
        return if (state?.manageSecurityState == null) {
            CardSettingsScreenState(
                cardDetails = null,
                accessCodeRecoveryState = null,
                onElementClick = {},
                onScanCardClick = {
                    store.dispatch(DetailsAction.ScanCard)
                },
            )
        } else {
            val cardId = if (state.card.isTangemTwins) {
                state.card.getTwinCardIdForUser()
            } else {
                state.cardInfo.cardId
            }
            val cardDetails: MutableList<CardInfo> = mutableListOf(
                CardInfo.CardId(cardId),
                CardInfo.Issuer(state.cardInfo.issuer),
            )

            if (!state.card.isTangemTwins) {
                cardDetails.add(CardInfo.SignedHashes(state.cardInfo.signedHashes.toString()))
            }
            cardDetails.add(
                CardInfo.SecurityMode(
                    securityOption = state.manageSecurityState.currentOption,
                    clickable = state.manageSecurityState.allowedOptions.size > 1,
                ),
            )
            if (state.card.backupStatus?.isActive == true && state.card.isAccessCodeSet) {
                cardDetails.add(CardInfo.ChangeAccessCode)
            }
            if (state.accessCodeRecovery != null) {
                cardDetails.add(CardInfo.AccessCodeRecovery(state.accessCodeRecovery.enabledOnCard))
            }
            if (state.resetCardAllowed) {
                cardDetails.add(CardInfo.ResetToFactorySettings(state.cardInfo))
            }
            CardSettingsScreenState(
                cardDetails = cardDetails,
                accessCodeRecoveryState = state.accessCodeRecovery,
                onScanCardClick = { },
                onElementClick = {
                    handleClickingItem(it)
                },
            )
        }
    }

    private fun handleClickingItem(item: CardInfo) {
        when (item) {
            is CardInfo.ChangeAccessCode -> {
                Analytics.send(Settings.CardSettings.ButtonChangeUserCode(AnalyticsParam.UserCode.AccessCode))
                store.dispatch(DetailsAction.ManageSecurity.ChangeAccessCode)
            }
            is CardInfo.ResetToFactorySettings -> {
                Analytics.send(Settings.CardSettings.ButtonFactoryReset())
                store.dispatch(DetailsAction.ResetToFactory.Start)
            }
            is CardInfo.SecurityMode -> {
                Analytics.send(Settings.CardSettings.ButtonChangeSecurityMode())
                store.dispatch(DetailsAction.ManageSecurity.OpenSecurity)
            }
            is CardInfo.AccessCodeRecovery -> {
                store.dispatch(DetailsAction.AccessCodeRecovery.Open)
            }
            else -> {}
        }
    }
}