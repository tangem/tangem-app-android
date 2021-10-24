package com.tangem.tap.features.details.redux


import com.tangem.common.card.Card
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.TapWorkarounds.isStart2Coin
import com.tangem.tap.domain.extensions.isWalletDataSupported
import com.tangem.tap.domain.extensions.signedHashesCount
import com.tangem.tap.domain.extensions.toSendableAmounts
import com.tangem.tap.domain.twins.getTwinCardNumber
import com.tangem.tap.domain.twins.isTangemTwin
import com.tangem.tap.features.details.redux.twins.CreateTwinWalletReducer
import com.tangem.tap.features.details.redux.twins.CreateTwinWalletState
import com.tangem.tap.features.wallet.models.hasPendingTransactions
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
        is DetailsAction.CreateTwinWalletAction -> {
            CreateTwinWalletReducer.handle(action, detailsState)
        }
        else -> detailsState
    }
}

private fun handlePrepareScreen(action: DetailsAction.PrepareScreen, state: DetailsState): DetailsState {
    val twinsState = if (action.card.isTangemTwin()) {
        CreateTwinWalletState(
                scanResponse = action.scanResponse,
                twinCardNumber = action.card.getTwinCardNumber(),
                createTwinWallet = null,
                showAlert = false,
                allowRecreatingWallet = action.isCreatingTwinWalletAllowed
        )
    } else {
        null
    }

    return DetailsState(
            card = action.card, wallets = action.wallets,
            cardInfo = action.card.toCardInfo(),
            appCurrencyState = AppCurrencyState(
                    action.fiatCurrencyName
            ),
            createTwinWalletState = twinsState,
            cardTermsOfUseUrl = action.cardTou.getUrl(action.card)
    )
}

private fun handleEraseWallet(action: DetailsAction.EraseWallet, state: DetailsState): DetailsState {
    return when (action) {
        DetailsAction.EraseWallet.Check -> {
            val notAllowedByAnyWallet = state.card?.wallets?.any { it.settings.isPermanent } ?: false
            val notAllowedByCard = notAllowedByAnyWallet || state.card?.isWalletDataSupported == true
            val notEmpty = state.wallets.any {
                it.hasPendingTransactions() || it.amounts.toSendableAmounts().isNotEmpty()
            }
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
        is DetailsAction.ManageSecurity.SetCurrentOption -> {
            val securityOption = when {
                action.userCodes.isAccessCodeSet -> {
                    SecurityOption.AccessCode
                }
                action.userCodes.isPasscodeSet -> {
                    SecurityOption.PassCode
                }
                else -> {
                    SecurityOption.LongTap
                }
            }
            state.copy(securityScreenState = SecurityScreenState(currentOption = securityOption))
        }
        is DetailsAction.ManageSecurity.OpenSecurity -> {
            if (state.card?.isStart2Coin == true) {
                return state.copy(securityScreenState = state.securityScreenState?.copy(
                        allowedOptions = EnumSet.of(SecurityOption.LongTap),
                        selectedOption = state.securityScreenState.currentOption
                ))
            }

            val allowedSecurityOptions = prepareAllowedSecurityOptions(
                state.card, state.securityScreenState?.currentOption
            )
            state.copy(securityScreenState = state.securityScreenState?.copy(
                    allowedOptions = allowedSecurityOptions,
                    selectedOption = state.securityScreenState.currentOption
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
            // Setting options to show only LongTap from now on for non-twins
            state.copy(
                securityScreenState = state.securityScreenState?.copy(
                    currentOption = state.securityScreenState.selectedOption,
                    allowedOptions = state.card?.let {
                        prepareAllowedSecurityOptions(
                            it, state.securityScreenState.selectedOption
                        )
                    } ?: EnumSet.of(SecurityOption.LongTap)
                ))
        }

        else -> state
    }
}

private fun prepareAllowedSecurityOptions(
    card: Card?, currentSecurityOption: SecurityOption?
): EnumSet<SecurityOption> {
    val prohibitDefaultPin = card?.settings?.isResettingUserCodesAllowed != true

    val prohibitDefaultPin = card?.settings?.isRemovingAccessCodeAllowed != true
    val allowedSecurityOptions = EnumSet.noneOf(SecurityOption::class.java)

    if (card?.isTwinCard() == true) {
        allowedSecurityOptions.add(SecurityOption.PassCode)
    }
    if ((currentSecurityOption == SecurityOption.LongTap) || !prohibitDefaultPin) {
        allowedSecurityOptions.add(SecurityOption.LongTap)
    }
    if (currentSecurityOption == SecurityOption.AccessCode) {
        allowedSecurityOptions.add(SecurityOption.AccessCode)
    }
    if (currentSecurityOption == SecurityOption.PassCode) {
        allowedSecurityOptions.add(SecurityOption.PassCode)
    }
    return allowedSecurityOptions
}


private fun Card.toCardInfo(): CardInfo? {
    val cardId = this.cardId.chunked(4).joinToString(separator = " ")
    val issuer = this.issuer.name
    val signedHashes = this.signedHashesCount()
    return CardInfo(cardId, issuer, signedHashes)}