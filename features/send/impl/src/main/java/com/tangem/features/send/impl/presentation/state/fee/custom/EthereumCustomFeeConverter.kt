package com.tangem.features.send.impl.presentation.state.fee.custom

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.blockchain.common.transaction.Fee
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
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.math.RoundingMode

internal class EthereumCustomFeeConverter(
    private val clickIntents: SendClickIntents,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Converter<Fee.Ethereum, ImmutableList<SendTextField.CustomFee>> {

    override fun convert(value: Fee.Ethereum): ImmutableList<SendTextField.CustomFee> {
        return persistentListOf(
            SendTextField.CustomFee(
                value = value.amount.value?.parseBigDecimal(value.amount.decimals).orEmpty(),
                decimals = value.amount.decimals,
                symbol = value.amount.currencySymbol,
                onValueChange = { clickIntents.onCustomFeeValueChange(FEE_AMOUNT, it) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Number,
                ),
                title = resourceReference(R.string.send_max_fee),
                footer = resourceReference(R.string.send_max_fee_footer),
                label = getFeeFormatted(value.amount.value),
                keyboardActions = KeyboardActions(),
            ),
            SendTextField.CustomFee(
                value = value.gasPrice.toString(),
                decimals = ETHEREUM_GAS_DECIMALS,
                symbol = ETHEREUM_GAS_UNIT,
                title = resourceReference(R.string.send_gas_price),
                footer = resourceReference(R.string.send_gas_price_footer),
                onValueChange = { clickIntents.onCustomFeeValueChange(GAS_PRICE, it) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Number,
                ),
                keyboardActions = KeyboardActions(),
            ),
            SendTextField.CustomFee(
                value = value.gasLimit.toString(),
                decimals = GAS_DECIMALS,
                symbol = null,
                title = resourceReference(R.string.send_gas_limit),
                footer = resourceReference(R.string.send_gas_limit_footer),
                onValueChange = { clickIntents.onCustomFeeValueChange(GAS_LIMIT, it) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number,
                ),
                keyboardActions = KeyboardActions(onDone = { clickIntents.onNextClick() }),
            ),
        )
    }

    fun convertBack(normalFee: Fee.Ethereum, value: ImmutableList<SendTextField.CustomFee>): Fee.Ethereum {
        val feeAmount = value[FEE_AMOUNT].value.parseToBigDecimal(value[FEE_AMOUNT].decimals)
        val gasPrice = value[GAS_PRICE].value.parseToBigDecimal(GAS_DECIMALS).toBigInteger()
        val gasLimit = value[GAS_LIMIT].value.parseToBigDecimal(GAS_DECIMALS).toBigInteger()
        return normalFee.copy(
            amount = normalFee.amount.copy(value = feeAmount),
            gasPrice = gasPrice,
            gasLimit = gasLimit,
        )
    }

    fun onValueChange(
        customValues: ImmutableList<SendTextField.CustomFee>,
        index: Int,
        value: String,
    ): ImmutableList<SendTextField.CustomFee> {
        val mutableCustomValues = customValues.toMutableList()
        return mutableCustomValues.apply {
            val gasLimit = this[GAS_LIMIT].value.parseToBigDecimal(this[GAS_LIMIT].decimals)
            when (index) {
                FEE_AMOUNT -> {
                    val newFeeAmountDecimal = value.parseToBigDecimal(this[FEE_AMOUNT].decimals)
                    val newFeeAmount = newFeeAmountDecimal.movePointRight(this[FEE_AMOUNT].decimals)
                    val newGasPrice = newFeeAmount.divide(gasLimit, GAS_DECIMALS, RoundingMode.HALF_UP)
                    set(GAS_PRICE, this[GAS_PRICE].copy(value = newGasPrice.parseBigDecimal(GAS_DECIMALS)))
                    set(
                        index,
                        this[index].copy(
                            value = value,
                            label = getFeeFormatted(newFeeAmountDecimal),
                        ),
                    )
                }
                GAS_PRICE -> {
                    val newGasPrice = value.parseToBigDecimal(this[GAS_PRICE].decimals)
                        .movePointLeft(this[GAS_PRICE].decimals)
                    val newFeeAmount = gasLimit * newGasPrice
                    set(
                        FEE_AMOUNT,
                        this[FEE_AMOUNT].copy(
                            value = newFeeAmount.parseBigDecimal(this[FEE_AMOUNT].decimals),
                            label = getFeeFormatted(newFeeAmount),
                        ),
                    )
                    set(index, this[index].copy(value = value))
                }
                else -> {
                    val newGasLimit = value.parseToBigDecimal(this[GAS_LIMIT].decimals)
                    val gasPrice = this[GAS_PRICE].value.parseToBigDecimal(this[GAS_PRICE].decimals)
                        .movePointLeft(this[FEE_AMOUNT].decimals)
                    val newFeeAmount = newGasLimit * gasPrice
                    set(
                        FEE_AMOUNT,
                        this[FEE_AMOUNT].copy(
                            value = newFeeAmount.parseBigDecimal(this[FEE_AMOUNT].decimals),
                            label = getFeeFormatted(newFeeAmount),
                        ),
                    )
                    set(index, this[index].copy(value = value))
                }
            }
        }.toImmutableList()
    }

    private fun getFeeFormatted(fee: BigDecimal?): TextReference {
        val appCurrency = appCurrencyProvider()
        val rate = cryptoCurrencyStatusProvider().value.fiatRate
        val fiatFee = rate?.let { fee?.multiply(it) }
        return stringReference(
            BigDecimalFormatter.formatFiatAmount(
                fiatAmount = fiatFee,
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
            ),
        )
    }

    companion object {
        private const val ETHEREUM_GAS_UNIT = "GWEI"
        private const val ETHEREUM_GAS_DECIMALS = 18
        private const val FEE_AMOUNT = 0
        private const val GAS_PRICE = 1
        private const val GAS_LIMIT = 2
        private const val GAS_DECIMALS = 0
    }
}
