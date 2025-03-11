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
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.RoundingMode

internal class KaspaCustomFeeConverter(
    private val clickIntents: SendClickIntents,
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
            )
        } else {
            persistentListOf()
        }
    }

    override fun convertBack(normalFee: Fee.Kaspa, value: ImmutableList<SendTextField.CustomFee>): Fee.Kaspa {
        val decimals = value[FEE_AMOUNT_INDEX].decimals
        val feeAmount = value[FEE_AMOUNT_INDEX].value.parseToBigDecimal(decimals)
        return normalFee.copy(
            amount = normalFee.amount.copy(value = feeAmount),
            mass = normalFee.mass,
            feeRate = feeAmount
                .divide(normalFee.mass.toBigDecimal(), decimals, RoundingMode.HALF_UP)
                .movePointRight(decimals)
                .toBigInteger(),
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
                FEE_AMOUNT_INDEX -> {
                    val valueDecimal = value.parseToBigDecimal(this[FEE_AMOUNT_INDEX].decimals)
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
            }
        }.toImmutableList()
    }

    fun tryAutoFixValue(
        minimumFee: Fee.Kaspa,
        customValues: ImmutableList<SendTextField.CustomFee>,
    ): ImmutableList<SendTextField.CustomFee> {
        val mutableCustomValues = customValues.toMutableList()
        val minimumFeeAmountValue = minimumFee.amount.value

        return mutableCustomValues.apply {
            // check that there is reveal transaction info (= krc-20 token transfer)
            // return without changes otherwise
            if (minimumFee.revealTransactionFee != null && minimumFeeAmountValue != null) {
                getOrNull(FEE_AMOUNT_INDEX)?.let {
                    val valueDecimal = it.value.parseToBigDecimal(it.decimals)
                    // krc-20 transaction will be failed if custom fee value is less than minimum,
                    // so we set value to minimum in this case
                    if (valueDecimal < minimumFee.amount.value) {
                        val fixedValue = minimumFeeAmountValue.parseBigDecimal(it.decimals)
                        set(
                            FEE_AMOUNT_INDEX,
                            it.copy(
                                value = fixedValue,
                                label = getFiatReference(
                                    rate = feeCryptoCurrencyStatusProvider()?.value?.fiatRate,
                                    value = valueDecimal,
                                    appCurrency = appCurrencyProvider(),
                                ),
                            ),
                        )
                    }
                }
            }
        }.toImmutableList()
    }

    private companion object {
        private const val FEE_AMOUNT_INDEX = 0
    }
}