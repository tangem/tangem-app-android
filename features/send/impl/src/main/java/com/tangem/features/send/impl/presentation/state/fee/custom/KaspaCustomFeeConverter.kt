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

internal class KaspaCustomFeeConverter(
    private val clickIntents: SendClickIntents,
    private val stateRouterProvider: Provider<StateRouter>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val feeCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
) : CustomFeeConverter<Fee.Kaspa> {

    override fun convert(value: Fee.Kaspa): ImmutableList<SendTextField.CustomFee> {
        val feeValue = value.amount.value
        val feeCurrency = feeCryptoCurrencyStatusProvider()?.value
        val network = feeCryptoCurrencyStatusProvider()?.currency?.network?.id?.value
        return if (network != null) {
            persistentListOf(
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
                    footer = resourceReference(R.string.send_custom_amount_fee_footer),
                    label = getFiatReference(
                        rate = feeCurrency?.fiatRate,
                        value = feeValue,
                        appCurrency = appCurrencyProvider(),
                    ),
                    keyboardActions = KeyboardActions(),
                ),
                SendTextField.CustomFee(
                    value = value.valuePerUtxo.parseBigDecimal(value.amount.decimals),
                    decimals = value.amount.decimals,
                    symbol = "",
                    title = resourceReference(R.string.send_custom_kaspa_per_utxo_title),
                    footer = resourceReference(R.string.send_custom_kaspa_per_utxo_footer),
                    onValueChange = { clickIntents.onCustomFeeValueChange(FEE_VALUE_PER_UTXO_INDEX, it) },
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (checkExceedBalance(
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
        } else {
            persistentListOf()
        }
    }

    override fun convertBack(normalFee: Fee.Kaspa, value: ImmutableList<SendTextField.CustomFee>): Fee.Kaspa {
        val feeAmount = value[FEE_AMOUNT_INDEX].value.parseToBigDecimal(value[FEE_AMOUNT_INDEX].decimals)
        val valuePerUtxo =
            value[FEE_VALUE_PER_UTXO_INDEX].value.parseToBigDecimal(value[FEE_VALUE_PER_UTXO_INDEX].decimals)
        return normalFee.copy(
            amount = normalFee.amount.copy(value = feeAmount),
            valuePerUtxo = valuePerUtxo,
        )
    }

    fun onValueChange(
        customValues: ImmutableList<SendTextField.CustomFee>,
        index: Int,
        value: String,
        utxoCount: Int,
    ): ImmutableList<SendTextField.CustomFee> {
        val mutableCustomValues = customValues.toMutableList()
        return mutableCustomValues.apply {
            when (index) {
                FEE_AMOUNT_INDEX -> {
                    val valueDecimal = value.parseToBigDecimal(this[FEE_AMOUNT_INDEX].decimals)
                    val newValuePerUtxo = valueDecimal.divide(
                        /* divisor = */ utxoCount.toBigDecimal(),
                        /* scale = */ this[FEE_VALUE_PER_UTXO_INDEX].decimals,
                        /* roundingMode = */ RoundingMode.HALF_UP,
                    )
                    set(
                        FEE_VALUE_PER_UTXO_INDEX,
                        this[FEE_VALUE_PER_UTXO_INDEX].copy(
                            value = newValuePerUtxo.parseBigDecimal(this[FEE_VALUE_PER_UTXO_INDEX].decimals),
                        ),
                    )
                    set(
                        index,
                        this[index].copy(
                            value = value,
                            label = getFiatReference(
                                rate = feeCryptoCurrencyStatusProvider()?.value?.fiatRate,
                                value = valueDecimal,
                                appCurrency = appCurrencyProvider(),
                            ),
                        ),
                    )
                }
                FEE_VALUE_PER_UTXO_INDEX -> {
                    val valuePerUtxo = value.parseToBigDecimal(this[FEE_VALUE_PER_UTXO_INDEX].decimals)
                    val newFeeAmount = valuePerUtxo.multiply(utxoCount.toBigDecimal())
                    set(
                        FEE_AMOUNT_INDEX,
                        this[FEE_AMOUNT_INDEX].copy(
                            value = newFeeAmount.parseBigDecimal(this[FEE_AMOUNT_INDEX].decimals),
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
        }.toImmutableList()
    }

    private companion object {
        private const val FEE_AMOUNT_INDEX = 0
        private const val FEE_VALUE_PER_UTXO_INDEX = 1
    }
}