package com.tangem.features.send.impl.presentation.state.fee.custom

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.amountScreen.utils.getFiatReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.fee.checkExceedBalance
import com.tangem.features.send.impl.presentation.state.fee.custom.EthereumCustomFeeConverter.Companion.ETHEREUM_GAS_UNIT
import com.tangem.features.send.impl.presentation.state.fee.custom.EthereumCustomFeeConverter.Companion.FEE_AMOUNT
import com.tangem.features.send.impl.presentation.state.fee.custom.EthereumCustomFeeConverter.Companion.GAS_DECIMALS
import com.tangem.features.send.impl.presentation.state.fee.custom.EthereumCustomFeeConverter.Companion.GIGA_DECIMALS
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.model.SendClickIntents
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.RoundingMode

internal class EthereumLegacyCustomFeeConverter(
    private val clickIntents: SendClickIntents,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val feeCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
) : BaseEthereumCustomFeeConverter<Fee.Ethereum.Legacy> {

    override fun convert(value: Fee.Ethereum.Legacy): ImmutableList<SendTextField.CustomFee> {
        return persistentListOf(
            SendTextField.CustomFee(
                value = value.gasPrice.toBigDecimal().movePointLeft(GIGA_DECIMALS).parseBigDecimal(GIGA_DECIMALS),
                decimals = GIGA_DECIMALS,
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
        )
    }

    override fun convertBack(
        normalFee: Fee.Ethereum.Legacy,
        value: ImmutableList<SendTextField.CustomFee>,
    ): Fee.Ethereum.Legacy {
        val feeAmount = value[FEE_AMOUNT].value.parseToBigDecimal(value[FEE_AMOUNT].decimals)
        val gasPrice = value[GAS_PRICE].value.parseToBigDecimal(GAS_DECIMALS).toBigInteger()
        val gasLimit = value[GAS_LIMIT].value.parseToBigDecimal(GAS_DECIMALS).toBigInteger()

        return normalFee.copy(
            amount = normalFee.amount.copy(value = feeAmount),
            gasPrice = gasPrice,
            gasLimit = gasLimit,
        )
    }

    override fun getGasLimitIndex(feeValue: Fee.Ethereum.Legacy): Int = GAS_LIMIT

    override fun onValueChange(
        feeValue: Fee.Ethereum.Legacy,
        customValues: ImmutableList<SendTextField.CustomFee>,
        index: Int,
        value: String,
    ): ImmutableList<SendTextField.CustomFee> {
        val mutableCustomValues = customValues.toMutableList()
        return mutableCustomValues.apply {
            when (index) {
                FEE_AMOUNT -> setOnAmountChange(value, index)
                GAS_PRICE -> setOnGasPriceChange(value, index)
                GAS_LIMIT -> setOnGasLimitChange(value, index)
                else -> set(index, this[index].copy(value = value))
            }
        }.toImmutableList()
    }

    private fun MutableList<SendTextField.CustomFee>.setOnAmountChange(value: String, index: Int) {
        val gasLimit = this[GAS_LIMIT].value.parseToBigDecimal(this[GAS_LIMIT].decimals)
        if (value.isBlank()) {
            setEmpty(FEE_AMOUNT)
            setEmpty(GAS_PRICE)
        } else {
            val newFeeAmountDecimal = value.parseToBigDecimal(this[FEE_AMOUNT].decimals)
            val newFeeAmount = newFeeAmountDecimal.movePointRight(this[GAS_PRICE].decimals) // from ETH to GWEI
            val newGasPrice = newFeeAmount.divide(gasLimit, this[GAS_PRICE].decimals, RoundingMode.HALF_UP)
            set(GAS_PRICE, this[GAS_PRICE].copy(value = newGasPrice.parseBigDecimal(this[GAS_PRICE].decimals)))
            set(
                index,
                this[index].copy(
                    value = value,
                    label = getFiatReference(
                        rate = feeCryptoCurrencyStatusProvider()?.value?.fiatRate,
                        value = newFeeAmountDecimal,
                        appCurrency = appCurrencyProvider(),
                    ),
                ),
            )
        }
    }

    private fun MutableList<SendTextField.CustomFee>.setOnGasPriceChange(value: String, index: Int) {
        val gasLimit = this[GAS_LIMIT].value.parseToBigDecimal(this[GAS_LIMIT].decimals)
        if (value.isBlank()) {
            setEmpty(FEE_AMOUNT)
            setEmpty(GAS_PRICE)
        } else {
            val newGasPrice = value.parseToBigDecimal(this[GAS_PRICE].decimals)
                .movePointLeft(this[GAS_PRICE].decimals) // from GWEI to ETH
            val newFeeAmount = gasLimit * newGasPrice
            set(
                FEE_AMOUNT,
                this[FEE_AMOUNT].copy(
                    value = newFeeAmount.parseBigDecimal(this[FEE_AMOUNT].decimals),
                    label = getFiatReference(
                        rate = feeCryptoCurrencyStatusProvider()?.value?.fiatRate,
                        value = newFeeAmount,
                        appCurrency = appCurrencyProvider(),
                    ),
                ),
            )
            set(index, this[index].copy(value = value))
        }
    }

    private fun MutableList<SendTextField.CustomFee>.setOnGasLimitChange(value: String, index: Int) {
        if (value.isBlank()) {
            setEmpty(FEE_AMOUNT)
            setEmpty(GAS_LIMIT)
        } else {
            val newGasLimit = value.parseToBigDecimal(this[GAS_LIMIT].decimals)
            val gasPrice = this[GAS_PRICE].value.parseToBigDecimal(this[GAS_PRICE].decimals)
                .movePointLeft(this[GAS_PRICE].decimals) // from GWEI to ETH

            val newFeeAmount = newGasLimit * gasPrice

            set(
                index = FEE_AMOUNT,
                element = this[FEE_AMOUNT].copy(
                    value = newFeeAmount.parseBigDecimal(this[FEE_AMOUNT].decimals),
                    label = getFiatReference(
                        rate = feeCryptoCurrencyStatusProvider()?.value?.fiatRate,
                        value = newFeeAmount,
                        appCurrency = appCurrencyProvider(),
                    ),
                ),
            )

            val isNotExceedBalance = checkExceedBalance(
                feeBalance = feeCryptoCurrencyStatusProvider()?.value?.amount,
                feeAmount = newFeeAmount,
            )

            set(
                index = index,
                element = this[index].copy(
                    value = value,
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (!isNotExceedBalance) ImeAction.None else ImeAction.Done,
                        keyboardType = KeyboardType.Number,
                    ),
                ),
            )
        }
    }

    private companion object {
        const val GAS_PRICE = 1
        const val GAS_LIMIT = 2
    }
}