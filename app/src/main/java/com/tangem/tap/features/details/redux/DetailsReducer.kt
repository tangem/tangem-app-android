package com.tangem.tap.features.details.redux


import com.tangem.common.card.Card
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.common.isTangemTwin
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.extensions.isWalletDataSupported
import com.tangem.tap.domain.extensions.signedHashesCount
import com.tangem.tap.features.wallet.models.hasSendableAmountsOrPendingTransactions
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
        is DetailsAction.ResetToFactory -> {
            handleEraseWallet(action, detailsState)
        }
        is DetailsAction.ManageSecurity -> {
            handleSecurityAction(action, detailsState)
        }
        else -> detailsState
    }
}

private fun handlePrepareScreen(
    action: DetailsAction.PrepareScreen,
    state: DetailsState,
): DetailsState {

    return DetailsState(
        scanResponse = action.scanResponse,
        wallets = action.wallets,
        cardInfo = action.scanResponse.card.toCardInfo(),
        cardTermsOfUseUrl = action.cardTou.getUrl(action.scanResponse.card),
        createBackupAllowed = action.scanResponse.card.backupStatus == Card.BackupStatus.NoBackup,
    )
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
        DetailsAction.ResetToFactory.Success -> state.copy(eraseWalletState = null)
        else -> state
    }
}

private fun handleSecurityAction(
    action: DetailsAction.ManageSecurity, state: DetailsState,
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
            val allowedSecurityOptions = when {
                state.scanResponse?.card?.isStart2Coin == true ||
                    state.scanResponse?.isTangemNote() == true -> {
                    EnumSet.of(SecurityOption.LongTap)
                }
                state.scanResponse?.supportsBackup() == true -> {
                    EnumSet.of(state.securityScreenState?.currentOption)
                }
                else -> prepareAllowedSecurityOptions(
                    state.scanResponse?.card, state.securityScreenState?.currentOption
                )
            }
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
                    allowedOptions = state.scanResponse?.card?.let {
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
