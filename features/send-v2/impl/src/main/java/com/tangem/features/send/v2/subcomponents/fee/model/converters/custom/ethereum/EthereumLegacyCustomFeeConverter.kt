package com.tangem.features.send.v2.subcomponents.fee.model.converters.custom.ethereum

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
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.subcomponents.fee.model.checkExceedBalance
import com.tangem.features.send.v2.subcomponents.fee.model.converters.custom.ethereum.EthereumCustomFeeConverter.Companion.ETHEREUM_GAS_UNIT
import com.tangem.features.send.v2.subcomponents.fee.model.converters.custom.ethereum.EthereumCustomFeeConverter.Companion.FEE_AMOUNT
import com.tangem.features.send.v2.subcomponents.fee.model.converters.custom.ethereum.EthereumCustomFeeConverter.Companion.GAS_DECIMALS
import com.tangem.features.send.v2.subcomponents.fee.model.converters.custom.ethereum.EthereumCustomFeeConverter.Companion.GIGA_DECIMALS
import com.tangem.features.send.v2.subcomponents.fee.model.converters.custom.setEmpty
import com.tangem.features.send.v2.subcomponents.fee.ui.state.CustomFeeFieldUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.RoundingMode

internal class EthereumLegacyCustomFeeConverter(
    private val onCustomFeeValueChange: (Int, String) -> Unit,
    private val appCurrency: AppCurrency,
    feeCryptoCurrencyStatus: CryptoCurrencyStatus,
) : BaseEthereumCustomFeeConverter<Fee.Ethereum.Legacy> {

    private val currencyStatus = feeCryptoCurrencyStatus.value

    override fun convert(value: Fee.Ethereum.Legacy): ImmutableList<CustomFeeFieldUM> {
        return persistentListOf(
            CustomFeeFieldUM(
                value = value.gasPrice.toBigDecimal().movePointLeft(GIGA_DECIMALS).parseBigDecimal(GIGA_DECIMALS),
                decimals = GIGA_DECIMALS,
                symbol = ETHEREUM_GAS_UNIT,
                title = resourceReference(R.string.send_gas_price),
                footer = resourceReference(R.string.send_gas_price_footer),
                onValueChange = { onCustomFeeValueChange(GAS_PRICE, it) },
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
        value: ImmutableList<CustomFeeFieldUM>,
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
        customValues: ImmutableList<CustomFeeFieldUM>,
        index: Int,
        value: String,
    ): ImmutableList<CustomFeeFieldUM> {
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

    private fun MutableList<CustomFeeFieldUM>.setOnAmountChange(value: String, index: Int) {
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
                        rate = currencyStatus.fiatRate,
                        value = newFeeAmountDecimal,
                        appCurrency = appCurrency,
                    ),
                ),
            )
        }
    }

    private fun MutableList<CustomFeeFieldUM>.setOnGasPriceChange(value: String, index: Int) {
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
                        rate = currencyStatus.fiatRate,
                        value = newFeeAmount,
                        appCurrency = appCurrency,
                    ),
                ),
            )
            set(index, this[index].copy(value = value))
        }
    }

    private fun MutableList<CustomFeeFieldUM>.setOnGasLimitChange(value: String, index: Int) {
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
                        rate = currencyStatus.fiatRate,
                        value = newFeeAmount,
                        appCurrency = appCurrency,
                    ),
                ),
            )

            val isNotExceedBalance = checkExceedBalance(
                feeBalance = currencyStatus.amount,
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