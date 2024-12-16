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
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.RoundingMode

internal class EthereumEIPCustomFeeConverter(
    private val clickIntents: SendClickIntents,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val feeCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
) : BaseEthereumCustomFeeConverter<Fee.Ethereum.EIP1559> {

    override fun convert(value: Fee.Ethereum.EIP1559): ImmutableList<SendTextField.CustomFee> {
        return persistentListOf(
            SendTextField.CustomFee(
                value = value.maxFeePerGas.toBigDecimal().movePointLeft(GIGA_DECIMALS).parseBigDecimal(GIGA_DECIMALS),
                decimals = GIGA_DECIMALS,
                symbol = ETHEREUM_GAS_UNIT,
                title = resourceReference(R.string.send_custom_evm_max_fee),
                footer = resourceReference(R.string.send_custom_evm_max_fee_footer),
                onValueChange = { clickIntents.onCustomFeeValueChange(MAX_FEE, it) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(),
            ),
            SendTextField.CustomFee(
                value = value.priorityFee.toBigDecimal().movePointLeft(GIGA_DECIMALS).parseBigDecimal(GIGA_DECIMALS),
                decimals = GIGA_DECIMALS,
                symbol = ETHEREUM_GAS_UNIT,
                title = resourceReference(R.string.send_custom_evm_priority_fee),
                footer = resourceReference(R.string.send_custom_evm_priority_fee_footer),
                onValueChange = { clickIntents.onCustomFeeValueChange(PRIORITY_FEE, it) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(),
            ),
        )
    }

    override fun convertBack(
        normalFee: Fee.Ethereum.EIP1559,
        value: ImmutableList<SendTextField.CustomFee>,
    ): Fee.Ethereum.EIP1559 {
        val feeAmount = value[FEE_AMOUNT].value.parseToBigDecimal(value[FEE_AMOUNT].decimals)
        val maxFeeDecimals = value[MAX_FEE].decimals
        val maxFee = value[MAX_FEE].value.parseToBigDecimal(maxFeeDecimals)
            .movePointRight(maxFeeDecimals)
            .toBigInteger()
        val priorityFeeDecimals = value[PRIORITY_FEE].decimals
        val priorityFee = value[PRIORITY_FEE].value.parseToBigDecimal(priorityFeeDecimals)
            .movePointRight(priorityFeeDecimals)
            .toBigInteger()
        val gasLimit = value[GAS_LIMIT].value.parseToBigDecimal(GAS_DECIMALS).toBigInteger()

        return normalFee.copy(
            amount = normalFee.amount.copy(value = feeAmount),
            maxFeePerGas = maxFee,
            priorityFee = priorityFee,
            gasLimit = gasLimit,
        )
    }

    override fun getGasLimitIndex(feeValue: Fee.Ethereum.EIP1559): Int = GAS_LIMIT

    override fun onValueChange(
        feeValue: Fee.Ethereum.EIP1559,
        customValues: ImmutableList<SendTextField.CustomFee>,
        index: Int,
        value: String,
    ): ImmutableList<SendTextField.CustomFee> {
        val mutableCustomValues = customValues.toMutableList()
        return mutableCustomValues.apply {
            when (index) {
                FEE_AMOUNT -> setOnAmountChange(value, index)
                MAX_FEE -> setOnMaxFeeChange(value, index)
                GAS_LIMIT -> setOnGasLimitChange(value, index)
                else -> set(index, this[index].copy(value = value))
            }
        }.toImmutableList()
    }

    private fun MutableList<SendTextField.CustomFee>.setOnAmountChange(value: String, index: Int) {
        val gasLimit = this[GAS_LIMIT].value.parseToBigDecimal(this[GAS_LIMIT].decimals)
        if (value.isBlank()) {
            setEmpty(FEE_AMOUNT)
            setEmpty(MAX_FEE)
        } else {
            val newFeeAmountDecimal = value.parseToBigDecimal(this[FEE_AMOUNT].decimals)
            val newFeeAmount = newFeeAmountDecimal.movePointRight(GIGA_DECIMALS) // from ETH to GWEI

            val newMaxFee = newFeeAmount.divide(gasLimit, this[MAX_FEE].decimals, RoundingMode.HALF_UP)

            set(
                index = MAX_FEE,
                element = this[MAX_FEE].copy(value = newMaxFee.parseBigDecimal(this[MAX_FEE].decimals)),
            )

            set(
                index = index,
                element = this[index].copy(
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

    private fun MutableList<SendTextField.CustomFee>.setOnMaxFeeChange(value: String, index: Int) {
        val gasLimit = this[GAS_LIMIT].value.parseToBigDecimal(this[GAS_LIMIT].decimals)

        if (value.isBlank()) {
            setEmpty(FEE_AMOUNT)
            setEmpty(MAX_FEE)
        } else {
            val newMaxFee = value.parseToBigDecimal(this[MAX_FEE].decimals).movePointLeft(this[MAX_FEE].decimals)
            val newFeeAmount = gasLimit * newMaxFee
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

            val maxFee = this[MAX_FEE].value.parseToBigDecimal(this[MAX_FEE].decimals)
                .movePointLeft(this[MAX_FEE].decimals) // from GWEI to ETH

            val newFeeAmount = newGasLimit * maxFee

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
        const val MAX_FEE = 1
        const val PRIORITY_FEE = 2
        const val GAS_LIMIT = 3
    }
}