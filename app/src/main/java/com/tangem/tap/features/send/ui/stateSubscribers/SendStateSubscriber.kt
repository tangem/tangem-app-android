package com.tangem.tap.features.send.ui.stateSubscribers

import android.app.Dialog
import android.content.Context
import android.text.SpannableStringBuilder
import android.view.View
import android.view.ViewGroup
import androidx.core.text.bold
import com.tangem.common.extensions.remove
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.tap.common.entities.ProgressState
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.getMessageString
import com.tangem.tap.common.text.DecimalDigitsInputFilter
import com.tangem.tap.domain.MultiMessageError
import com.tangem.tap.domain.assembleErrors
import com.tangem.tap.features.BaseStoreFragment
import com.tangem.tap.features.send.redux.AddressVerifyAction.Error
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.features.send.redux.states.*
import com.tangem.tap.features.send.ui.FeeUiHelper
import com.tangem.tap.features.send.ui.SendFragment
import com.tangem.tap.features.send.ui.SendViewModel
import com.tangem.tap.features.send.ui.adapters.WarningMessagesAdapter
import com.tangem.tap.features.send.ui.dialogs.*
import com.tangem.wallet.R

/**
 * Created by Anton Zhilenkov on 31/08/2020.
 */
@Suppress("LargeClass")
internal class SendStateSubscriber(
    fragment: BaseStoreFragment,
) : FragmentStateSubscriber<SendState>(fragment) {

    private var dialog: Dialog? = null
    private var sendViewModel: SendViewModel? = null
    fun initViewModel(viewModel: SendViewModel) {
        sendViewModel = viewModel
    }

    override fun updateWithNewState(fg: BaseStoreFragment, state: SendState) {
        fg.view ?: return
        if (fg !is SendFragment) return
        if (state.isSuccessSend) {
            sendViewModel?.updateCurrencyDelayed()
            return
        }

        val lastChangedStates = state.lastChangedStates.toList()
        state.lastChangedStates.clear()
        (fg as? SendFragment)?.binding?.mainSendContainer?.beginDelayedTransition()
        lastChangedStates.forEach {
            when (it) {
                StateId.SEND_SCREEN -> handleSendScreen(fg, state)
                StateId.ADDRESS_PAY_ID -> handleAddressState(fg, state.addressState)
                StateId.TRANSACTION_EXTRAS -> handleTransactionExtrasState(fg, state.transactionExtrasState)
                StateId.AMOUNT -> handleAmountState(fg, state.amountState)
                StateId.FEE -> handleFeeState(fg, state.feeState)
                StateId.RECEIPT -> handleReceiptState(fg, state.receiptState, state.feeState.progressState)
            }
        }
    }

    @Deprecated("Legacy")
    @Suppress("ComplexMethod", "LongMethod")
    private fun handleTransactionExtrasState(fg: SendFragment, infoState: TransactionExtrasState) =
        with(fg.binding.lSendAddress) {
            fun showView(view: View, info: Any?) {
                view.show(info != null)
            }
            showView(xlmMemoContainer, infoState.xlmMemo)
            showView(xrpDestinationTagContainer, infoState.xrpDestinationTag)
            showView(binanceMemoContainer, infoState.binanceMemo)
            showView(tonMemoContainer, infoState.tonMemoState)
            showView(cosmosMemoContainer, infoState.cosmosMemoState)
            showView(hederaMemoContainer, infoState.hederaMemoState)
            showView(algorandMemoContainer, infoState.algorandMemoState)

            infoState.xlmMemo?.let {
                if (!it.viewFieldValue.isFromUserInput) etXlmMemo.setText(it.viewFieldValue.value)
                if (it.error != null) {
                    if (it.error == TransactionExtraError.INVALID_XLM_MEMO) {
                        tilXlmMemo.error = fg.getText(R.string.send_extras_error_invalid_memo)
                    }
                } else {
                    tilXlmMemo.error = null
                }
            }
            infoState.xrpDestinationTag?.let {
                if (infoState.xrpDestinationTag.error != null) {
                    if (infoState.xrpDestinationTag.error == TransactionExtraError.INVALID_DESTINATION_TAG) {
                        tilDestinationTag.error =
                            fg.getText(R.string.send_extras_error_invalid_destination_tag)
                    }
                } else {
                    tilDestinationTag.error = null
                }
                if (!it.viewFieldValue.isFromUserInput) {
                    etDestinationTag.setText(it.viewFieldValue.value)
                }
            }
            infoState.binanceMemo?.let {
                if (infoState.binanceMemo.error != null) {
                    if (infoState.binanceMemo.error == TransactionExtraError.INVALID_BINANCE_MEMO) {
                        tilBinanceMemo.error = fg.getText(R.string.send_extras_error_invalid_memo)
                    }
                } else {
                    tilBinanceMemo.error = null
                }
                if (!it.viewFieldValue.isFromUserInput) {
                    etBinanceMemo.setText(it.viewFieldValue.value)
                }
            }
            infoState.tonMemoState?.let {
                if (infoState.tonMemoState.error != null) {
                    tilTonMemo.error = fg.getText(R.string.send_extras_error_invalid_memo)
                } else {
                    tilBinanceMemo.error = null
                }
                if (!it.viewFieldValue.isFromUserInput) {
                    etTonMemo.setText(it.viewFieldValue.value)
                }
            }
            infoState.cosmosMemoState?.let {
                if (infoState.cosmosMemoState.error != null) {
                    tilCosmosMemo.error = fg.getText(R.string.send_extras_error_invalid_memo)
                } else {
                    tilBinanceMemo.error = null
                }
                if (!it.viewFieldValue.isFromUserInput) {
                    etCosmosMemo.setText(it.viewFieldValue.value)
                }
            }
            infoState.hederaMemoState?.let {
                if (infoState.hederaMemoState.error != null) {
                    tilHederaMemo.error = fg.getText(R.string.send_extras_error_invalid_memo)
                } else {
                    tilHederaMemo.error = null
                }
                if (!it.viewFieldValue.isFromUserInput) {
                    etHederaMemo.setText(it.viewFieldValue.value)
                }
            }
            infoState.algorandMemoState?.let {
                if (infoState.algorandMemoState.error != null) {
                    tilAlgorandMemo.error = fg.getText(R.string.send_extras_error_invalid_memo)
                } else {
                    tilAlgorandMemo.error = null
                }
                if (!it.viewFieldValue.isFromUserInput) {
                    etAlgorandMemo.setText(it.viewFieldValue.value)
                }
            }
        }

    @Suppress("ComplexMethod")
    private fun handleSendScreen(fg: SendFragment, state: SendState) = with(fg.binding) {
        when (state.dialog) {
            is SendAction.Dialog.TezosWarningDialog -> {
                if (dialog == null) {
                    dialog = TezosWarningDialog.create(fg.requireContext(), state.dialog)
                    dialog?.show()
                }
            }
            is SendAction.Dialog.KaspaWarningDialog -> {
                if (dialog == null) {
                    dialog = KaspaWarningDialog.create(fg.requireContext(), state.dialog)
                    dialog?.show()
                }
            }
            is SendAction.Dialog.ChiaWarningDialog -> {
                if (dialog == null) {
                    dialog = ChiaWarningDialog.create(fg.requireContext(), state.dialog)
                    dialog?.show()
                }
            }
            is SendAction.Dialog.SendTransactionFails.CardSdkError -> {
                if (dialog == null) {
                    dialog = SendTransactionFailsDialog.create(fg.requireContext(), state.dialog)
                    dialog?.show()
                }
            }
            is SendAction.Dialog.SendTransactionFails.BlockchainSdkError -> {
                if (dialog == null) {
                    dialog = SendTransactionFailsDialog.create(fg.requireContext(), state.dialog)
                    dialog?.show()
                }
            }
            is SendAction.Dialog.RequestFeeError -> {
                if (dialog == null) {
                    dialog = RequestFeeErrorDialog.create(fg.requireContext(), state.dialog)
                    dialog?.show()
                }
            }
            else -> {
                dialog?.dismiss()
                dialog = null
            }
        }

        fg.sendBtn.changeState(state.sendButtonState.progressState)
        fg.sendBtn.mainView.isEnabled = state.sendButtonState.enabled

        val rv = rvWarningMessages
        val adapter = rv.adapter as? WarningMessagesAdapter ?: return

        adapter.submitList(state.sendWarningsList)
        rv.show(state.sendWarningsList.isNotEmpty())

        toolbar.title = fg.getString(
            R.string.send_title_currency_format,
            state.amountState.mainCurrency.currencySymbol,
        )
    }

    private fun handleAddressState(fg: SendFragment, state: AddressState) {
        with(fg.binding.lSendAddress) {
            fun parseError(context: Context, error: Error?): String? {
                val resId = when (error) {
                    Error.ADDRESS_INVALID_OR_UNSUPPORTED_BY_BLOCKCHAIN -> R.string.send_validation_invalid_address
                    Error.ADDRESS_SAME_AS_WALLET -> R.string.send_error_address_same_as_wallet
                    else -> null
                }
                return if (resId == null) null else context.getString(resId, "", "")
            }

            imvPaste.isEnabled = state.pasteIsEnabled

            val et = etAddress
            val til = tilAddress
            val parsedError = parseError(til.context, state.error)

            til.isEnabled = state.inputIsEnabled
            imvPaste.show(state.inputIsEnabled)
            imvQrCode.show(state.inputIsEnabled)
            flPaste.show(state.inputIsEnabled)
            flQrCode.show(state.inputIsEnabled)

            til.hint = til.getString(R.string.send_destination_hint_address)
            til.error = parsedError
            til.isErrorEnabled = parsedError != null
            til.helperText = state.destinationWalletAddress
            til.isHelperTextEnabled = parsedError == null

            if (!state.viewFieldValue.isFromUserInput) et.update(state.viewFieldValue.value)
        }
    }

    private fun handleAmountState(fg: SendFragment, state: AmountState) = with(fg.binding.lSendAmount) {
        if (state.error != null) {
            val context = fg.requireContext()
            val message = when (state.error) {
                is MultiMessageError -> {
                    val multiError = state.error as MultiMessageError
                    val messageList = multiError.assembleErrors()
                        .map { getMessageString(context, it.first, it.second) }
                    multiError.builder(messageList)
                }
                else -> context.getString(state.error.messageResource)
            }
            tilAmountToSend.enableError(true, message)
        } else {
            tilAmountToSend.enableError(false)
        }

        val filter = DecimalDigitsInputFilter(
            digitsBeforeDecimal = 40,
            digitsAfterDecimal = state.maxLengthOfAmount,
            decimalSeparator = state.decimalSeparator,
        )
        etAmountToSend.filters = arrayOf(filter)
        val amountToSend = state.viewAmountValue
        if (!amountToSend.isFromUserInput) etAmountToSend.update(amountToSend.value)

        tvAmountCurrency.update(state.mainCurrency.currencySymbol)
        (fg as? SendFragment)?.saveMainCurrency(state.mainCurrency.type)

        val balanceText = when (state.mainCurrency.type) {
            MainCurrencyType.FIAT -> fg.getString(
                R.string.common_balance,
                "${state.viewBalanceValue} ${state.mainCurrency.currencySymbol}",
            ).remove(":")
            MainCurrencyType.CRYPTO -> fg.getString(
                R.string.common_balance,
                "${state.mainCurrency.currencySymbol} ${state.viewBalanceValue}",
            )
        }

        tvBalance.update(balanceText)

        tilAmountToSend.isEnabled = state.inputIsEnabled

        val imageRes = if (state.inputIsEnabled) R.drawable.ic_arrows_up_down else 0
        tvAmountCurrency.setCompoundDrawablesWithIntrinsicBounds(0, 0, imageRes, 0)
        val textColor = if (state.inputIsEnabled) R.color.accent else R.color.text_secondary
        tvAmountCurrency.setTextColor(fg.getColor(textColor))
    }

    @Suppress("MagicNumber")
    private fun handleFeeState(fg: SendFragment, state: FeeState) {
        with(fg.binding.clNetworkFee) {
            fg.view?.findViewById<ViewGroup>(R.id.clNetworkFee)?.show(state.mainLayoutIsVisible)
            flExpandCollapse.imvExpandCollapse.rotation = if (state.controlsLayoutIsVisible) 0f else 180f
            llFeeControlsContainer.show(state.controlsLayoutIsVisible)
            chipGroup.show(state.feeChipGroupIsVisible)

            swIncludeFee.isEnabled = state.includeFeeSwitcherIsEnabled
            if (swIncludeFee.isChecked != state.feeIsIncluded) {
                swIncludeFee.isChecked = state.feeIsIncluded
            }

            val chipId = FeeUiHelper.toId(state.selectedFeeType)
            if (chipGroup.checkedChipId != chipId && chipId != View.NO_ID) chipGroup.check(chipId)
        }
        with(fg.binding.clReceiptContainer) {
            when (state.progressState) {
                ProgressState.Loading -> {
                    tvReceiptFeeValue.hide()
                    pbReceiptFee.show()
                }
                ProgressState.Done -> {
                    pbReceiptFee.hide()
                    tvReceiptFeeValue.show()
                }
                else -> {}
            }
        }
    }

    @Suppress("LongMethod", "ComplexMethod")
    private fun handleReceiptState(fg: SendFragment, state: ReceiptState, feeProgressState: ProgressState) {
        with(fg.binding.clReceiptContainer) {
            val mainLayout = clReceiptContainer as ViewGroup
            val totalLayout = llTotalContainer.llTotal as ViewGroup
            val totalTokenLayout = llTotalContainer.flTotalTokenCrypto as ViewGroup

            fun getString(id: Int, vararg formatStrings: String): String = mainLayout.getString(id, *formatStrings)

            fun roughOrEmpty(value: String): String {
                return if (value == BigDecimalFormatter.EMPTY_BALANCE_SIGN) value else "$ROUGH_SIGN $value"
            }

            when (feeProgressState) {
                ProgressState.Loading -> {
                    tvReceiptFeeValue.hide()
                    pbReceiptFee.show()
                }
                ProgressState.Done -> {
                    pbReceiptFee.hide()
                    tvReceiptFeeValue.show()
                }
                else -> {}
            }

            when (state.visibleTypeOfReceipt) {
                ReceiptLayoutType.FIAT -> {
                    val receipt = state.fiat ?: return

                    totalLayout.show(true)
                    totalTokenLayout.show(false)
                    tvReceiptAmountValue.update("${receipt.amountFiat} ${receipt.symbols.fiat}")
                    tvReceiptFeeValue.update("${receipt.feeFiat} ${receipt.symbols.fiat}")
                    llTotalContainer.tvTotalValue.post {
                        llTotalContainer.tvTotalValue.update(
                            "${roughOrEmpty(receipt.totalFiat)} ${receipt.symbols.fiat}",
                        )
                    }

                    val willSent = getString(
                        R.string.send_total_subtitle_format,
                        "${receipt.willSentCrypto} ${receipt.symbols.crypto}",
                    )
                    llTotalContainer.tvWillBeSentValue.update(willSent)
                }
                ReceiptLayoutType.CRYPTO -> {
                    val receipt = state.crypto ?: return

                    totalLayout.show(true)
                    totalTokenLayout.show(false)
                    tvReceiptAmountValue.update("${receipt.amountCrypto} ${receipt.symbols.crypto}")
                    tvReceiptFeeValue.update("${receipt.feeCrypto} ${receipt.symbols.crypto}")
                    llTotalContainer.tvTotalValue.post {
                        llTotalContainer.tvTotalValue.update("${receipt.totalCrypto} ${receipt.symbols.crypto}")
                    }

                    if (receipt.willSentFiat == BigDecimalFormatter.EMPTY_BALANCE_SIGN) {
                        llTotalContainer.tvWillBeSentValue.hide()
                    } else {
                        llTotalContainer.tvWillBeSentValue.show()
                        llTotalContainer.tvWillBeSentValue.update(
                            getString(
                                R.string.send_total_subtitle_fiat_format,
                                "${receipt.willSentFiat} ${receipt.symbols.fiat}",
                                "${receipt.feeFiat} ${receipt.symbols.fiat}",
                            ),
                        )
                    }
                }
                ReceiptLayoutType.TOKEN_FIAT -> {
                    val receipt = state.tokenFiat ?: return

                    totalLayout.show(true)
                    totalTokenLayout.show(false)
                    tvReceiptAmountValue.update("${receipt.amountFiat} ${receipt.symbols.fiat}")
                    tvReceiptFeeValue.update("${receipt.feeFiat} ${receipt.symbols.fiat}")
                    llTotalContainer.tvTotalValue.post {
                        llTotalContainer.tvTotalValue.update(
                            "${roughOrEmpty(receipt.totalFiat)} ${receipt.symbols.fiat}",
                        )
                    }

                    val willSent = getString(
                        R.string.send_total_subtitle_asset_format,
                        "${receipt.symbols.token ?: ""} ${receipt.willSentToken}",
                        "${receipt.symbols.crypto} ${receipt.willSentFeeCoin}",
                    )
                    llTotalContainer.tvWillBeSentValue.update(willSent)
                }
                ReceiptLayoutType.TOKEN_CRYPTO -> {
                    val receipt = state.tokenCrypto ?: return

                    totalLayout.show(false)
                    totalTokenLayout.show(true)

                    tvReceiptAmountValue.update("${receipt.amountToken} ${receipt.symbols.token}")
                    tvReceiptFeeValue.update("${receipt.feeCoin} ${receipt.symbols.crypto}")

                    val willSent = SpannableStringBuilder()
                        .bold {
                            append(roughOrEmpty(receipt.totalFiat)).append(" ")
                            append(receipt.symbols.fiat)
                        }
                    llTotalContainer.tvTotalTokenCryptoValue.update(willSent.toString())
                }
                ReceiptLayoutType.FEE_IN_CUSTOM_TOKEN -> {
                    val receipt = state.customTokenCrypto ?: return

                    totalLayout.show(false)
                    totalTokenLayout.show(true)

                    tvReceiptAmountValue.update(
                        "${receipt.amountToken} ${receipt.symbols.token ?: receipt.symbols.crypto}",
                    )
                    tvReceiptFeeValue.update("${receipt.feeCoin} ${receipt.symbols.fee}")

                    val willSent = SpannableStringBuilder()
                        .bold {
                            append(roughOrEmpty(receipt.totalFiat)).append(" ")
                            append(receipt.symbols.fiat)
                        }
                    llTotalContainer.tvTotalTokenCryptoValue.update(willSent.toString())
                }
                ReceiptLayoutType.FEE_IN_CUSTOM_TOKEN_FIAT -> {
                    val receipt = state.customTokenFiat ?: return

                    totalLayout.show(true)
                    totalTokenLayout.show(false)
                    tvReceiptAmountValue.update("${receipt.amountFiat} ${receipt.symbols.fiat}")
                    tvReceiptFeeValue.update("${receipt.feeFiat} ${receipt.symbols.fiat}")
                    llTotalContainer.tvTotalValue.post {
                        llTotalContainer.tvTotalValue.update(
                            "${roughOrEmpty(receipt.totalFiat)} ${receipt.symbols.fiat}",
                        )
                    }

                    val willSent = getString(
                        R.string.send_total_subtitle_asset_format,
                        "${receipt.symbols.token ?: receipt.symbols.crypto} ${receipt.willSentToken}",
                        "${receipt.symbols.fee} ${receipt.willSentFeeCoin}",
                    )
                    llTotalContainer.tvWillBeSentValue.update(willSent)
                }

                ReceiptLayoutType.SAME_CURRENCY -> {
                    val receipt = state.sameCurrencyCrypto ?: return

                    totalLayout.show(true)
                    totalTokenLayout.show(false)
                    tvReceiptAmountValue.update(
                        "${receipt.amountCrypto} ${receipt.symbols.token ?: receipt.symbols.crypto}",
                    )
                    tvReceiptFeeValue.update("${receipt.feeCrypto} ${receipt.symbols.token ?: receipt.symbols.crypto}")
                    llTotalContainer.tvTotalValue.post {
                        llTotalContainer.tvTotalValue.update(
                            "${receipt.totalCrypto} ${receipt.symbols.token ?: receipt.symbols.crypto}",
                        )
                    }

                    if (receipt.willSentFiat == BigDecimalFormatter.EMPTY_BALANCE_SIGN) {
                        llTotalContainer.tvWillBeSentValue.hide()
                    } else {
                        llTotalContainer.tvWillBeSentValue.show()
                        llTotalContainer.tvWillBeSentValue.update(
                            getString(
                                R.string.send_total_subtitle_fiat_format,
                                "${receipt.willSentFiat} ${receipt.symbols.fiat}",
                                "${receipt.feeFiat} ${receipt.symbols.fiat}",
                            ),
                        )
                    }
                }
                ReceiptLayoutType.SAME_CURRENCY_FIAT -> {
                    val receipt = state.sameCurrencyFiat ?: return

                    totalLayout.show(true)
                    totalTokenLayout.show(false)
                    tvReceiptAmountValue.update("${receipt.amountFiat} ${receipt.symbols.fiat}")
                    tvReceiptFeeValue.update("${receipt.feeFiat} ${receipt.symbols.fiat}")
                    llTotalContainer.tvTotalValue.post {
                        llTotalContainer.tvTotalValue.update(
                            "${roughOrEmpty(receipt.totalFiat)} ${receipt.symbols.fiat}",
                        )
                    }

                    val willSent = getString(
                        R.string.send_total_subtitle_format,
                        "${receipt.willSentCrypto} ${receipt.symbols.token ?: receipt.symbols.crypto}",
                    )
                    llTotalContainer.tvWillBeSentValue.update(willSent)
                }
                ReceiptLayoutType.UNKNOWN, null -> {}
            }
        }
    }

    private companion object {
        const val ROUGH_SIGN = "≈"
    }
}
