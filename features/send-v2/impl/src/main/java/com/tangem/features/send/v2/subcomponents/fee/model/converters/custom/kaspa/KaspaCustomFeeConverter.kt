package com.tangem.features.send.v2.subcomponents.fee.model.converters.custom.kaspa

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
import com.tangem.features.send.v2.subcomponents.fee.model.converters.custom.CustomFeeConverter
import com.tangem.features.send.v2.api.entity.CustomFeeFieldUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.RoundingMode

internal class KaspaCustomFeeConverter(
    private val onCustomFeeValueChange: (Int, String) -> Unit,
    private val appCurrency: AppCurrency,
    feeCryptoCurrencyStatus: CryptoCurrencyStatus,
) : CustomFeeConverter<Fee.Kaspa> {

    private val currencyStatus = feeCryptoCurrencyStatus.value

    override fun convert(value: Fee.Kaspa): ImmutableList<CustomFeeFieldUM> {
        val feeValue = value.amount.value
        return persistentListOf(
            CustomFeeFieldUM(
                value = feeValue?.parseBigDecimal(value.amount.decimals).orEmpty(),
                decimals = value.amount.decimals,
                symbol = value.amount.currencySymbol,
                onValueChange = { onCustomFeeValueChange(FEE_AMOUNT_INDEX, it) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Companion.Next,
                    keyboardType = KeyboardType.Companion.Number,
                ),
                title = resourceReference(R.string.send_max_fee),
                footer = resourceReference(R.string.send_custom_amount_fee_footer),
                label = getFiatReference(
                    rate = currencyStatus.fiatRate,
                    value = feeValue,
                    appCurrency = appCurrency,
                ),
                keyboardActions = KeyboardActions(),
            ),
        )
    }

    override fun convertBack(normalFee: Fee.Kaspa, value: ImmutableList<CustomFeeFieldUM>): Fee.Kaspa {
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
        customValues: ImmutableList<CustomFeeFieldUM>,
        index: Int,
        value: String,
    ): ImmutableList<CustomFeeFieldUM> {
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
                                rate = currencyStatus.fiatRate,
                                value = valueDecimal,
                                appCurrency = appCurrency,
                            ),
                        ),
                    )
                }
            }
        }.toImmutableList()
    }

    fun tryAutoFixValue(
        minimumFee: Fee.Kaspa,
        customValues: ImmutableList<CustomFeeFieldUM>,
    ): ImmutableList<CustomFeeFieldUM> {
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
                                    rate = currencyStatus.fiatRate,
                                    value = valueDecimal,
                                    appCurrency = appCurrency,
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