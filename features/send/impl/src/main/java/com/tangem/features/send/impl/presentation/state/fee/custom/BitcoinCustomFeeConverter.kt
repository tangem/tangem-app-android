package com.tangem.features.send.impl.presentation.state.fee.custom

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.extensions.isZero
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.math.RoundingMode

internal class BitcoinCustomFeeConverter(
    private val clickIntents: SendClickIntents,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val feeCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
) : CustomFeeConverter<Fee.Bitcoin> {

    override fun convert(value: Fee.Bitcoin): ImmutableList<SendTextField.CustomFee> {
        val feeValue = value.amount.value
        return persistentListOf(
            SendTextField.CustomFee(
                value = feeValue?.parseBigDecimal(value.amount.decimals).orEmpty(),
                decimals = value.amount.decimals,
                symbol = value.amount.currencySymbol,
                onValueChange = { clickIntents.onCustomFeeValueChange(FEE_AMOUNT_INDEX, it) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Number,
                ),
                title = resourceReference(R.string.send_max_fee),
                footer = resourceReference(R.string.send_max_fee_footer),
                label = getFeeFormatted(feeValue),
                keyboardActions = KeyboardActions(),
                isReadonly = true,
            ),
            SendTextField.CustomFee(
                value = toSatoshiPerByte(
                    amount = feeValue,
                    decimals = value.amount.decimals,
                    txSize = value.txSize,
                ).toString(),
                decimals = SATOSHI_DECIMALS,
                symbol = "",
                title = resourceReference(R.string.send_gas_price),
                footer = resourceReference(R.string.send_gas_price_footer),
                onValueChange = { clickIntents.onCustomFeeValueChange(FEE_SATOSHI_INDEX, it) },
                keyboardOptions = KeyboardOptions(
                    imeAction = if (checkExceedBalance(feeValue)) ImeAction.None else ImeAction.Done,
                    keyboardType = KeyboardType.Number,
                ),
                keyboardActions = KeyboardActions(),
            ),
        )
    }

    override fun convertBack(normalFee: Fee.Bitcoin, value: ImmutableList<SendTextField.CustomFee>): Fee.Bitcoin {
        val feeAmount = value[FEE_AMOUNT_INDEX].value.parseToBigDecimal(value[FEE_AMOUNT_INDEX].decimals)
        val satoshiPerByte = value[FEE_SATOSHI_INDEX].value.parseToBigDecimal(value[FEE_SATOSHI_INDEX].decimals)
        return normalFee.copy(
            amount = normalFee.amount.copy(value = feeAmount),
            satoshiPerByte = satoshiPerByte,
        )
    }

    fun onValueChange(
        customValues: ImmutableList<SendTextField.CustomFee>,
        index: Int,
        value: String,
        txSize: BigDecimal,
    ): ImmutableList<SendTextField.CustomFee> {
        val mutableCustomValues = customValues.toMutableList()
        return mutableCustomValues.apply {
            if (index == FEE_SATOSHI_INDEX) {
                val newSatoshiPerKb = value.parseToBigDecimal(this[FEE_SATOSHI_INDEX].decimals)
                val newFeeAmount = newSatoshiPerKb.multiply(txSize)
                    .movePointLeft(this[FEE_AMOUNT_INDEX].decimals)
                    .setScale(this[FEE_AMOUNT_INDEX].decimals, RoundingMode.DOWN)
                set(
                    FEE_AMOUNT_INDEX,
                    this[FEE_AMOUNT_INDEX].copy(
                        value = newFeeAmount.parseBigDecimal(this[FEE_AMOUNT_INDEX].decimals),
                        label = getFeeFormatted(newFeeAmount),
                    ),
                )
                set(index, this[index].copy(value = value))
            }
        }.toImmutableList()
    }

    private fun getFeeFormatted(fee: BigDecimal?): TextReference {
        val appCurrency = appCurrencyProvider()
        val rate = feeCryptoCurrencyStatusProvider()?.value?.fiatRate
        val fiatFee = rate?.let { fee?.multiply(it) }
        return stringReference(
            BigDecimalFormatter.formatFiatAmount(
                fiatAmount = fiatFee,
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
            ),
        )
    }

    private fun checkExceedBalance(feeAmount: BigDecimal?): Boolean {
        val cryptoCurrencyStatus = feeCryptoCurrencyStatusProvider()
        val currencyCryptoAmount = cryptoCurrencyStatus?.value?.amount ?: BigDecimal.ZERO

        return feeAmount == null || feeAmount.isZero() || feeAmount > currencyCryptoAmount
    }

    private fun toSatoshiPerByte(amount: BigDecimal?, decimals: Int, txSize: BigDecimal): BigDecimal? {
        val newFeeAmount = amount?.movePointRight(decimals)
        return newFeeAmount?.divide(
            txSize,
            SATOSHI_DECIMALS,
            RoundingMode.HALF_UP,
        )?.setScale(SATOSHI_DECIMALS, RoundingMode.HALF_UP)
    }

    private companion object {
        private const val FEE_AMOUNT_INDEX = 0
        private const val FEE_SATOSHI_INDEX = 1
        private const val SATOSHI_DECIMALS = 0
    }
}