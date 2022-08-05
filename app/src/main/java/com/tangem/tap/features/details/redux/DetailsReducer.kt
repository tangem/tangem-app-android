package com.tangem.tap.features.details.redux

import com.tangem.common.card.Card
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.common.TapWorkarounds.isTangemNote
import com.tangem.domain.common.isTangemTwin
import com.tangem.tap.common.analytics.AnalyticsEvent
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.extensions.isWalletDataSupported
import com.tangem.tap.domain.extensions.signedHashesCount
import com.tangem.tap.store
import org.rekotlin.Action
import java.util.*

class DetailsReducer {
    companion object {
        fun reduce(action: Action, state: AppState): DetailsState = internalReduce(action, state)
    }
}

private fun internalReduce(action: Action, state: AppState): DetailsState {
    if (action !is DetailsAction) return state.detailsState

    val detailsState = state.detailsState
    return when (action) {
        is DetailsAction.PrepareScreen -> {
            handlePrepareScreen(action)
        }
        is DetailsAction.PrepareCardSettingsData -> {
            handlePrepareCardSettingsScreen(action.card, detailsState)
        }
        is DetailsAction.ResetCardSettingsData -> detailsState.copy(cardSettingsState = null)
        is DetailsAction.ResetToFactory -> {
            handleEraseWallet(action, detailsState)
        }
        is DetailsAction.ManageSecurity -> {
            handleSecurityAction(action, detailsState)
        }
        is DetailsAction.AppSettings -> {
            handlePrivacyAction(action, detailsState)
        }
        is DetailsAction.ChangeAppCurrency ->
            detailsState.copy(appCurrency = action.fiatCurrency)

        else -> detailsState
    }
}

private fun handlePrepareScreen(
    action: DetailsAction.PrepareScreen,
): DetailsState {

    return DetailsState(
        scanResponse = action.scanResponse,
        wallets = action.wallets,
        cardTermsOfUseUrl = action.cardTou.getUrl(action.scanResponse.card),
        createBackupAllowed = action.scanResponse.card.backupStatus == Card.BackupStatus.NoBackup,
        appCurrency = store.state.globalState.appCurrency,
    )
}

private fun handlePrepareCardSettingsScreen(card: Card, state: DetailsState): DetailsState {
    val cardSettingsState = CardSettingsState(
        cardInfo = card.toCardInfo(),
        manageSecurityState = prepareSecurityOptions(card),
        card = card,
        resetCardAllowed = isResetToFactoryAllowedByCard(card),
    )
    return state.copy(cardSettingsState = cardSettingsState)
}

private fun prepareSecurityOptions(card: Card): ManageSecurityState {
    val securityOption = when {
        card.isAccessCodeSet -> {
            SecurityOption.AccessCode
        }
        card.isPasscodeSet == true -> {
            SecurityOption.PassCode
        }
        else -> {
            SecurityOption.LongTap
        }
    }
    val allowedSecurityOptions = when {
        card.isStart2Coin || card.isTangemNote() -> {
            EnumSet.of(SecurityOption.LongTap)
        }
        card.settings.isBackupAllowed -> {
            EnumSet.of(securityOption)
        }
        else -> prepareAllowedSecurityOptions(card, securityOption)
    }
    return ManageSecurityState(
        currentOption = securityOption,
        allowedOptions = allowedSecurityOptions,
        selectedOption = securityOption,
    )
}

private fun isResetToFactoryAllowedByCard(card: Card): Boolean {
    val notAllowedByAnyWallet = card.wallets.any { it.settings.isPermanent }
    val notAllowedByCard = notAllowedByAnyWallet ||
        (card.isWalletDataSupported && (!card.isTangemNote() && !card.settings.isBackupAllowed))
    return !notAllowedByCard
}

private fun handleEraseWallet(
    action: DetailsAction.ResetToFactory,
    state: DetailsState,
): DetailsState {
    return when (action) {
        DetailsAction.ResetToFactory.Check -> {
            val card = state.scanResponse?.card
            val notAllowedByAnyWallet = card?.wallets?.any { it.settings.isPermanent } ?: false
            val notAllowedByCard = notAllowedByAnyWallet ||
                (card?.isWalletDataSupported == true &&
                    (!state.scanResponse.isTangemNote() && !state.scanResponse.supportsBackup()))

            val notEmpty = state.wallets.any { it.hasSendableAmountsOrPendingTransactions() }
            val eraseWalletState = when {
                notAllowedByCard -> EraseWalletState.NotAllowedByCard
                notEmpty -> EraseWalletState.NotEmpty
                else -> EraseWalletState.Allowed
            }
            state.copy(eraseWalletState = eraseWalletState)
        }
        DetailsAction.ResetToFactory.Proceed -> {
            if (state.eraseWalletState == EraseWalletState.Allowed) {
                state.copy(confirmScreenState = ConfirmScreenState.EraseWallet)
            } else {
                state
            }
        }
        DetailsAction.ResetToFactory.Cancel -> state.copy(eraseWalletState = null)
        DetailsAction.ResetToFactory.Failure -> state.copy(eraseWalletState = null)
        DetailsAction.ResetToFactory.Success -> {
            store.state.globalState.analyticsHandlers?.triggerEvent(event = AnalyticsEvent.FACTORY_RESET_SUCCESS)
            state.copy(eraseWalletState = null)
        }
        is DetailsAction.ResetToFactory.Confirm ->
            state.copy(cardSettingsState = state.cardSettingsState?.copy(resetConfirmed = action.confirmed))
        else -> state
    }
}

private fun handleSecurityAction(
    action: DetailsAction.ManageSecurity, state: DetailsState,
): DetailsState {
    return when (action) {
        is DetailsAction.ManageSecurity.SelectOption -> {
            val manageSecurityState = state.cardSettingsState?.manageSecurityState?.copy(
                selectedOption = action.option,
            )
            state.copy(cardSettingsState = state.cardSettingsState?.copy(manageSecurityState = manageSecurityState))
        }
        is DetailsAction.ManageSecurity.SaveChanges.Success -> {
            // Setting options to show only LongTap from now on for non-twins
            val manageSecurityState = state.cardSettingsState?.manageSecurityState?.copy(
                currentOption = state.cardSettingsState.manageSecurityState.selectedOption,
                allowedOptions = state.scanResponse?.card?.let {
                    prepareAllowedSecurityOptions(
                        it, state.cardSettingsState.manageSecurityState.selectedOption,
                    )
                } ?: EnumSet.of(SecurityOption.LongTap),
            )
            state.copy(
                cardSettingsState = state.cardSettingsState?.copy(
                    manageSecurityState = manageSecurityState,
                ),
            )
        }

        else -> state
    }
}

private fun handlePrivacyAction(
    action: DetailsAction.AppSettings, state: DetailsState,
): DetailsState {
    return when (action) {
        is DetailsAction.AppSettings.ConfirmSwitchingSetting -> {
            when (action.setting) {
                PrivacySetting.SAVE_CARDS -> state.copy(saveCards = action.allow)
                PrivacySetting.SAVE_ACCESS_CODE -> state.copy(savePasswords = action.allow)
            }
        }
        is DetailsAction.AppSettings.SwitchPrivacySetting -> state
    }
}

private fun prepareAllowedSecurityOptions(
    card: Card?, currentSecurityOption: SecurityOption?,
): EnumSet<SecurityOption> {
    val allowedSecurityOptions = EnumSet.of(SecurityOption.LongTap)

    if (card?.isTangemTwin() == true) {
        allowedSecurityOptions.add(SecurityOption.PassCode)
    }
    if (currentSecurityOption == SecurityOption.AccessCode) {
        allowedSecurityOptions.add(SecurityOption.AccessCode)
    }
    if (currentSecurityOption == SecurityOption.PassCode) {
        allowedSecurityOptions.add(SecurityOption.PassCode)
    }
    return allowedSecurityOptions
}

private fun Card.toCardInfo(): CardInfo {
    val cardId = this.cardId.chunked(4).joinToString(separator = " ")
    val issuer = this.issuer.name
    val signedHashes = this.signedHashesCount()
    return CardInfo(cardId, issuer, signedHashes)
}
