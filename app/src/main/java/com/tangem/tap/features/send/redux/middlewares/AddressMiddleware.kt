package com.tangem.tap.features.send.redux.middlewares

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.core.analytics.Analytics
import com.tangem.tap.common.analytics.events.Token.Send.AddressEntered
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.send.redux.*
import com.tangem.tap.features.send.redux.AddressVerifyAction.AddressVerification.SetAddressError
import com.tangem.tap.features.send.redux.AddressVerifyAction.AddressVerification.SetWalletAddress
import com.tangem.tap.features.send.redux.AddressVerifyAction.Error
import com.tangem.tap.scope
import kotlinx.coroutines.*
import org.rekotlin.Action
import org.rekotlin.DispatchFunction

/**
[REDACTED_AUTHOR]
 */
internal class AddressMiddleware {

    val addressValidator = AddressValidator()

    fun handle(action: AddressActionUi, appState: AppState?, dispatch: (Action) -> Unit) {
        when (action) {
            is AddressActionUi.HandleUserInput -> handleUserInput(action.data, appState, dispatch)
            is AddressActionUi.PasteAddress -> pasteAddress(action.data, action.sourceType, dispatch)
            is AddressActionUi.CheckClipboard -> verifyClipboard(action.data, appState, dispatch)
            is AddressActionUi.CheckAddress -> verifyAddress(action.sourceType, appState, dispatch)
            else -> return
        }
    }

    private fun handleUserInput(input: String, appState: AppState?, dispatch: DispatchFunction) {
        val sendState = appState?.sendState ?: return
        if (input == sendState.addressState.viewFieldValue.value) return

        setAddressAndCheck(data = input, sourceType = null, isUserInput = true, dispatch = dispatch)
    }

    private fun pasteAddress(data: String, sourceType: AddressEntered.SourceType, dispatch: (Action) -> Unit) {
        setAddressAndCheck(data = data, sourceType = sourceType, isUserInput = false, dispatch = dispatch)
    }

    private fun setAddressAndCheck(
        data: String,
        sourceType: AddressEntered.SourceType?,
        isUserInput: Boolean,
        dispatch: (Action) -> Unit,
    ) {
        dispatch(SetWalletAddress(data, isUserInput))
        dispatch(AddressActionUi.CheckAddress(sourceType))
    }

    private fun verifyAddress(
        sourceType: AddressEntered.SourceType?,
        appState: AppState?,
        dispatch: (Action) -> Unit,
    ) {
        val sendState = appState?.sendState ?: return
        val walletManager = sendState.walletManager ?: return
        val address = sendState.addressState.normalFieldValue ?: return
        val isUserInput = sendState.addressState.viewFieldValue.isFromUserInput

        verifyAddress(
            address = address,
            walletManager = walletManager,
            isUserInput = isUserInput,
            dispatch = dispatch,
            sourceType = sourceType,
        )
    }

    private fun verifyAddress(
        address: String,
        walletManager: WalletManager,
        sourceType: AddressEntered.SourceType?,
        isUserInput: Boolean,
        dispatch: (Action) -> Unit,
    ) {
        val wallet = walletManager.wallet

        scope.launch(Dispatchers.Main) {
            val failReason = withContext(Dispatchers.IO) {
                addressValidator.validateAddress(walletManager, address)
            }

            if (failReason == null) {
                dispatchSuccessValidationActions(
                    address = address,
                    wallet = wallet,
                    sourceType = sourceType,
                    isUserInput = isUserInput,
                    dispatch = dispatch,
                )
            } else {
                dispatchFailedValidationActions(
                    failReason = failReason,
                    sourceType = sourceType,
                    dispatch = dispatch,
                )
            }
        }
    }

    private fun dispatchSuccessValidationActions(
        address: String,
        wallet: Wallet,
        sourceType: AddressEntered.SourceType?,
        isUserInput: Boolean,
        dispatch: (Action) -> Unit,
    ) {
        val addressSchemeSplit = when (wallet.blockchain) {
            Blockchain.BitcoinCash, Blockchain.Kaspa -> listOf(address)
            else -> address.split(":")
        }

        val noSchemeAddress = when (addressSchemeSplit.size) {
            1 -> address // no scheme
            2 -> { // scheme
                if (wallet.blockchain.validateShareScheme(addressSchemeSplit[0])) {
                    addressSchemeSplit[1]
                } else {
                    sourceType?.let {
                        Analytics.send(
                            event = AddressEntered(
                                sourceType = sourceType,
                                validationResult = AddressEntered.ValidationResult.Fail,
                            ),
                        )
                    }
                    dispatch(SetAddressError(Error.ADDRESS_INVALID_OR_UNSUPPORTED_BY_BLOCKCHAIN))
                    return
                }
            }
            else -> { // invalid URI
                sourceType?.let {
                    Analytics.send(
                        event = AddressEntered(
                            sourceType = sourceType,
                            validationResult = AddressEntered.ValidationResult.Fail,
                        ),
                    )
                }
                dispatch(SetAddressError(Error.ADDRESS_INVALID_OR_UNSUPPORTED_BY_BLOCKCHAIN))
                return
            }
        }

        val supposedAddress = noSchemeAddress.removeShareUriQuery() // TODO: parse query?

        noSchemeAddress.getQueryParameter("amount")?.toBigDecimalOrNull()?.let {
            dispatch(AmountAction.SetAmount(it, false))
            dispatch(AmountActionUi.CheckAmountToSend)
        }
        dispatch(SetWalletAddress(supposedAddress, isUserInput))
        dispatch(TransactionExtrasAction.Prepare(wallet.blockchain, address, null))
        dispatch(FeeAction.RequestFee)
        sourceType?.let {
            Analytics.send(
                event = AddressEntered(
                    sourceType = sourceType,
                    validationResult = AddressEntered.ValidationResult.Success,
                ),
            )
        }
    }

    private fun dispatchFailedValidationActions(
        failReason: AddressVerifyAction.Error,
        sourceType: AddressEntered.SourceType?,
        dispatch: (Action) -> Unit,
    ) {
        dispatch(SetAddressError(failReason))
        dispatch(TransactionExtrasAction.Release)
        sourceType?.let {
            Analytics.send(
                event = AddressEntered(
                    sourceType = sourceType,
                    validationResult = AddressEntered.ValidationResult.Fail,
                ),
            )
        }
    }

    private fun String.removeShareUriQuery(): String = this.substringBefore("?")

    private fun String.getQueryParameter(name: String): String? {
        return this.substringAfter("?").splitToMap("&", "=")[name]
    }

    private fun verifyClipboard(input: String?, appState: AppState?, dispatch: DispatchFunction) {
        val address = input ?: return
        val walletManager = appState?.sendState?.walletManager ?: return

        val internalDispatcher: (Action) -> Unit = {
            when (it) {
                is SetWalletAddress -> {
                    dispatch(AddressVerifyAction.ChangePasteBtnEnableState(true))
                }
                is SetAddressError -> {
                    dispatch(AddressVerifyAction.ChangePasteBtnEnableState(false))
                }
            }
        }

        verifyAddress(
            address = address,
            walletManager = walletManager,
            sourceType = null,
            isUserInput = false,
            dispatch = internalDispatcher,
        )
    }
}

fun String.splitToMap(firstDelimiter: String, secondDelimiter: String): Map<String, String> {
    return this
        .split(firstDelimiter)
        .map { it.split(secondDelimiter) }
        .associate { it.first() to it.last() }
}