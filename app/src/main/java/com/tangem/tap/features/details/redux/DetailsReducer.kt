package com.tangem.tap.features.details.redux

import com.tangem.commands.Card
import com.tangem.commands.Settings
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.extensions.toSendableAmounts
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
            handlePrepareScreen(action, detailsState)
        }
        is DetailsAction.EraseWallet -> {
            handleEraseWallet(action, detailsState)
        }
        is DetailsAction.AppCurrencyAction -> {
            handleAppCurrencyAction(action, detailsState)
        }
        is DetailsAction.ManageSecurity -> {
            handleSecurityAction(action, detailsState)
        }
    }
}

private fun handlePrepareScreen(action: DetailsAction.PrepareScreen, state: DetailsState): DetailsState {
    val securityOption = when {
        action.card.isPin1Default == false -> {
            SecurityOption.AccessCode
        }
        action.card.isPin2Default == false -> {
            SecurityOption.PassCode
        }
        else -> {
            SecurityOption.LongTap
        }
    }
    return DetailsState(
            card = action.card, wallet = action.wallet,
            cardInfo = action.card.toCardInfo(),
            appCurrencyState = AppCurrencyState(
                    action.fiatCurrencyName
            ),
            securityScreenState = SecurityScreenState(currentOption = securityOption)
    )
}

private fun handleEraseWallet(action: DetailsAction.EraseWallet, state: DetailsState): DetailsState {
    return when (action) {
        DetailsAction.EraseWallet.Check -> {
            val notAllowedByCard = state.card?.settingsMask?.contains(Settings.ProhibitPurgeWallet) == true
            val notEmpty = state.wallet?.recentTransactions?.isNullOrEmpty() != true ||
                    state.wallet.amounts.toSendableAmounts().isNotEmpty()
            val eraseWalletState = when {
                notAllowedByCard -> EraseWalletState.NotAllowedByCard
                notEmpty -> EraseWalletState.NotEmpty
                else -> EraseWalletState.Allowed
            }
            state.copy(eraseWalletState = eraseWalletState)
        }
        DetailsAction.EraseWallet.Proceed -> {
            if (state.eraseWalletState == EraseWalletState.Allowed) {
                state.copy(confirmScreenState = ConfirmScreenState.EraseWallet)
            } else {
                state
            }
        }
        DetailsAction.EraseWallet.Cancel -> state.copy(eraseWalletState = null)
        DetailsAction.EraseWallet.Failure -> state.copy(eraseWalletState = null)
        DetailsAction.EraseWallet.Success -> state.copy(eraseWalletState = null)
        else -> state
    }
}

private fun handleAppCurrencyAction(
        action: DetailsAction.AppCurrencyAction, state: DetailsState
): DetailsState {
    return when (action) {
        is DetailsAction.AppCurrencyAction.SetCurrencies -> {
            state.copy(appCurrencyState = state.appCurrencyState.copy(fiatCurrencies = action.currencies))
        }
        DetailsAction.AppCurrencyAction.ChooseAppCurrency -> {
            state.copy(appCurrencyState = state.appCurrencyState.copy(showAppCurrencyDialog = true))
        }
        DetailsAction.AppCurrencyAction.Cancel -> {
            state.copy(appCurrencyState = state.appCurrencyState.copy(showAppCurrencyDialog = false))
        }
        is DetailsAction.AppCurrencyAction.SelectAppCurrency -> {
            state.copy(
                    appCurrencyState = state.appCurrencyState.copy(
                            fiatCurrencyName = action.fiatCurrencyName, showAppCurrencyDialog = false
                    )
            )
        }
        else -> state
    }
}

private fun handleSecurityAction(
        action: DetailsAction.ManageSecurity, state: DetailsState
): DetailsState {
    return when (action) {
        is DetailsAction.ManageSecurity.OpenSecurity -> {
            if (state.card?.isPin2Default == null) {
                return state.copy(securityScreenState = state.securityScreenState?.copy(
                        allowedOptions = EnumSet.noneOf(SecurityOption::class.java)
                ))
            }
            val prohibitDefaultPin = state.card.settingsMask?.contains(Settings.ProhibitDefaultPIN1) == true
            val allowSetPin1 = state.card.settingsMask?.contains(Settings.AllowSetPIN1) != false
            val allowSetPin2 = state.card.settingsMask?.contains(Settings.AllowSetPIN2) != false
            val isDefaultPin1 = state.card.isPin1Default != false
            val isDefaultPin2 = state.card.isPin2Default != false

            val allowedSecurityOptions = EnumSet.noneOf(SecurityOption::class.java)
            if ((isDefaultPin1 && isDefaultPin2) || !prohibitDefaultPin) {
                allowedSecurityOptions.add(SecurityOption.LongTap)
            }
            if (allowSetPin1 && (isDefaultPin2 || !prohibitDefaultPin)) {
                allowedSecurityOptions.add(SecurityOption.AccessCode)
            }
            if (allowSetPin2 && (isDefaultPin1 || !prohibitDefaultPin)) {
                allowedSecurityOptions.add(SecurityOption.PassCode)
            }

            state.copy(securityScreenState = state.securityScreenState?.copy(
                    allowedOptions = allowedSecurityOptions
            ))
        }
        is DetailsAction.ManageSecurity.SelectOption -> {
            state.copy(securityScreenState = state.securityScreenState?.copy(
                    selectedOption = action.option
            ))
        }
        is DetailsAction.ManageSecurity.ConfirmSelection -> {
            val confirmScreenState = when (action.option) {
                SecurityOption.LongTap -> ConfirmScreenState.LongTap
                SecurityOption.PassCode -> ConfirmScreenState.PassCode
                SecurityOption.AccessCode -> ConfirmScreenState.AccessCode
            }
            state.copy(confirmScreenState = confirmScreenState)
        }
        is DetailsAction.ManageSecurity.SaveChanges.Success -> {
            state.copy(securityScreenState = state.securityScreenState?.copy(
                    currentOption = state.securityScreenState.selectedOption
            ))
        }

        else -> state
    }
}

private fun Card.toCardInfo(): CardInfo? {
    val cardId = this.cardId.chunked(4).joinToString(separator = " ")
    val issuer = this.cardData?.issuerName ?: return null
    val signedHashes = this.walletSignedHashes ?: return null
    return CardInfo(cardId, issuer, signedHashes)
}