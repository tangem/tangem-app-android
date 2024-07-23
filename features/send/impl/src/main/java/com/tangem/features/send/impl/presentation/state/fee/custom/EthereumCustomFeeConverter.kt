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
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.features.send.impl.presentation.state.fee.checkExceedBalance
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.RoundingMode

internal class EthereumCustomFeeConverter(
    private val clickIntents: SendClickIntents,
    private val stateRouterProvider: Provider<StateRouter>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val feeCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
) : CustomFeeConverter<Fee.Ethereum> {

    override fun convert(value: Fee.Ethereum): ImmutableList<SendTextField.CustomFee> {
        val feeValue = value.amount.value
        val feeCurrency = feeCryptoCurrencyStatusProvider()?.value
        return persistentListOf(
            SendTextField.CustomFee(
                value = feeValue?.parseBigDecimal(value.amount.decimals).orEmpty(),
                decimals = value.amount.decimals,
                symbol = value.amount.currencySymbol,
                onValueChange = { clickIntents.onCustomFeeValueChange(FEE_AMOUNT, it) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Number,
                ),
                title = resourceReference(R.string.send_max_fee),
                footer = resourceReference(R.string.send_custom_amount_fee_footer),
                label = getFiatReference(
                    rate = feeCurrency?.fiatRate,
                    value = feeValue,
                    appCurrency = appCurrencyProvider(),
                ),
                keyboardActions = KeyboardActions(),
            ),
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
            SendTextField.CustomFee(
                value = value.gasLimit.toString(),
                decimals = GAS_DECIMALS,
                symbol = "",
                title = resourceReference(R.string.send_gas_limit),
                footer = resourceReference(R.string.send_gas_limit_footer),
                onValueChange = { clickIntents.onCustomFeeValueChange(GAS_LIMIT, it) },
                keyboardOptions = KeyboardOptions(
                    imeAction = if (
                        checkExceedBalance(
                            feeBalance = feeCurrency?.amount,
                            feeAmount = feeValue,
                        )
                    ) {
                        ImeAction.None
                    } else {
                        ImeAction.Done
                    },
                    keyboardType = KeyboardType.Number,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { clickIntents.onNextClick(stateRouterProvider().isEditState) },
                ),
            ),
        )
    }

    override fun convertBack(normalFee: Fee.Ethereum, value: ImmutableList<SendTextField.CustomFee>): Fee.Ethereum {
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
            when (index) {
                FEE_AMOUNT -> setOnAmountChange(value, index)
                GAS_PRICE -> setOnGasPriceChange(value, index)
                else -> setOnGasLimitChange(value, index)
            }
        }.toImmutableList()
    }

    private fun MutableList<SendTextField.CustomFee>.setEmpty(index: Int) {
        set(index, this[index].copy(value = ""))
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
            val isNotExceedBalance = checkExceedBalance(
                feeBalance = feeCryptoCurrencyStatusProvider()?.value?.amount,
                feeAmount = newFeeAmount,
            )
            set(
                index,
                this[index].copy(
                    value = value,
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (!isNotExceedBalance) ImeAction.None else ImeAction.Done,
                        keyboardType = KeyboardType.Number,
                    ),
                ),
            )
        }
    }

    companion object {
        private const val ETHEREUM_GAS_UNIT = "GWEI"
        private const val GIGA_DECIMALS = 9
        private const val FEE_AMOUNT = 0
        private const val GAS_PRICE = 1
        private const val GAS_LIMIT = 2
        private const val GAS_DECIMALS = 0
    }
}