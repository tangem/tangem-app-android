package com.tangem.features.send.v2.send.confirm.model.transformers

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fee
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.send.confirm.ui.state.ConfirmUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

internal class SendConfirmInitialStateTransformer(
    private val appCurrency: AppCurrency,
    private val feeUM: FeeUM,
    private val amountUM: AmountState,
    private val isShowTapHelp: Boolean,
    private val isSubtracted: Boolean,
) : Transformer<ConfirmUM> {
    override fun transform(prevState: ConfirmUM): ConfirmUM {
        return ConfirmUM.Content(
            isSending = false,
            showTapHelp = isShowTapHelp,
            sendingFooter = getSendingFooterText(),
            notifications = persistentListOf(),
        )
    }

    private fun getSendingFooterText(): TextReference {
        val feeUM = feeUM as? FeeUM.Content
        val amountUM = amountUM as? AmountState.Data

        if (feeUM == null || amountUM == null) return TextReference.EMPTY

        val fee = (feeUM.feeSelectorUM as? FeeSelectorUM.Content)?.selectedFee
        val fiatAmountValue = amountUM.amountTextField.fiatAmount.value
        val fiatFeeValue = feeUM.rate?.let { fee?.amount?.value?.multiply(it) }

        val fiatSendingValue = if (isSubtracted) {
            fiatAmountValue
        } else {
            if (feeUM.isFeeConvertibleToFiat) {
                fiatFeeValue?.let { fiatAmountValue?.plus(it) }
            } else {
                fiatAmountValue
            }
        }

        val fiatSending = fiatSendingValue.format {
            fiat(
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
            )
        }
        val fiatFee = formatFiatFee(
            amount = fee?.amount,
            isFeeConvertibleToFiat = feeUM.isFeeConvertibleToFiat,
            isFeeApproximate = feeUM.isFeeApproximate,
        )

        return if (feeUM.isTronToken && fee is Fee.Tron) {
            getTokenFeeSendingText(
                fee = fee,
                fiatFee = fiatFee,
                fiatSending = fiatSending,
            )
        } else {
            resourceReference(
                id = if (feeUM.isFeeConvertibleToFiat) {
                    R.string.send_summary_transaction_description
                } else {
                    R.string.send_summary_transaction_description_no_fiat_fee
                },
                formatArgs = wrappedList(fiatSending, fiatFee),
            )
        }
    }

    private fun formatFiatFee(amount: Amount?, isFeeConvertibleToFiat: Boolean, isFeeApproximate: Boolean): String {
        return if (isFeeConvertibleToFiat) {
            amount?.value.format {
                fiat(
                    fiatCurrencyCode = appCurrency.code,
                    fiatCurrencySymbol = appCurrency.symbol,
                )
            }
        } else {
            amount?.value.format {
                crypto(
                    decimals = amount?.decimals ?: 0,
                    symbol = amount?.currencySymbol.orEmpty(),
                ).fee(
                    canBeLower = isFeeApproximate,
                )
            }
        }
    }

    private fun getTokenFeeSendingText(fee: Fee.Tron, fiatFee: String, fiatSending: String): TextReference {
        val suffix = when {
            fee.remainingEnergy == 0L -> {
                resourceReference(
                    R.string.send_summary_transaction_description_suffix_including,
                    wrappedList(fiatFee),
                )
            }
            fee.feeEnergy <= fee.remainingEnergy -> {
                resourceReference(
                    R.string.send_summary_transaction_description_suffix_fee_covered,
                    wrappedList(fee.feeEnergy),
                )
            }
            else -> {
                resourceReference(
                    R.string.send_summary_transaction_description_suffix_fee_reduced,
                    wrappedList(fee.remainingEnergy),
                )
            }
        }
        val prefix = resourceReference(
            R.string.send_summary_transaction_description_prefix,
            wrappedList(fiatSending),
        )

        return combinedReference(prefix, COMMA_SEPARATOR, suffix)
    }

    companion object {
        private val COMMA_SEPARATOR = stringReference(", ")
    }
}