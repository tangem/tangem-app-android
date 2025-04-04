package com.tangem.features.send.v2.subcomponents.fee.model.converters.custom.bitcoin

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
import com.tangem.features.send.v2.subcomponents.fee.model.SendFeeClickIntents
import com.tangem.features.send.v2.subcomponents.fee.model.checkExceedBalance
import com.tangem.features.send.v2.subcomponents.fee.model.converters.custom.CustomFeeConverter
import com.tangem.features.send.v2.subcomponents.fee.ui.state.CustomFeeFieldUM
import com.tangem.lib.crypto.BlockchainUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.math.RoundingMode

internal class BitcoinCustomFeeConverter(
    private val clickIntents: SendFeeClickIntents,
    private val appCurrency: AppCurrency,
    private val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
) : CustomFeeConverter<Fee.Bitcoin> {

    private val currencyStatus = feeCryptoCurrencyStatus.value
    private val network = feeCryptoCurrencyStatus.currency.network.id.value

    override fun convert(value: Fee.Bitcoin): ImmutableList<CustomFeeFieldUM> {
        val feeValue = value.amount.value
        return if (BlockchainUtils.isUseBitcoinFeeConverter(network)) {
            persistentListOf(
                CustomFeeFieldUM(
                    value = feeValue?.parseBigDecimal(value.amount.decimals).orEmpty(),
                    decimals = value.amount.decimals,
                    symbol = value.amount.currencySymbol,
                    onValueChange = { clickIntents.onCustomFeeValueChange(FEE_AMOUNT_INDEX, it) },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Companion.Next,
                        keyboardType = KeyboardType.Companion.Number,
                    ),
                    title = resourceReference(R.string.send_max_fee),
                    footer = resourceReference(R.string.send_bitcoin_custom_fee_footer),
                    label = getFiatReference(
                        rate = currencyStatus.fiatRate,
                        value = feeValue,
                        appCurrency = appCurrency,
                    ),
                    keyboardActions = KeyboardActions(),
                    isReadonly = true,
                ),
                CustomFeeFieldUM(
                    value = toSatoshiPerByte(
                        amount = feeValue,
                        decimals = value.amount.decimals,
                        txSize = value.txSize,
                    ).toString(),
                    decimals = SATOSHI_DECIMALS,
                    symbol = "",
                    title = resourceReference(R.string.send_satoshi_per_byte_title),
                    footer = resourceReference(R.string.send_satoshi_per_byte_text),
                    onValueChange = { clickIntents.onCustomFeeValueChange(FEE_SATOSHI_INDEX, it) },
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (checkExceedBalance(
                                feeBalance = currencyStatus.amount,
                                feeAmount = feeValue,
                            )
                        ) {
                            ImeAction.Companion.None
                        } else {
                            ImeAction.Companion.Done
                        },
                        keyboardType = KeyboardType.Companion.Number,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { clickIntents.onNextClick() },
                    ),
                ),
            )
        } else {
            persistentListOf()
        }
    }

    override fun convertBack(normalFee: Fee.Bitcoin, value: ImmutableList<CustomFeeFieldUM>): Fee.Bitcoin {
        val feeAmount = value[FEE_AMOUNT_INDEX].value.parseToBigDecimal(value[FEE_AMOUNT_INDEX].decimals)
        val satoshiPerByte = value[FEE_SATOSHI_INDEX].value.parseToBigDecimal(value[FEE_SATOSHI_INDEX].decimals)
        return normalFee.copy(
            amount = normalFee.amount.copy(value = feeAmount),
            satoshiPerByte = satoshiPerByte,
        )
    }

    fun onValueChange(
        customValues: ImmutableList<CustomFeeFieldUM>,
        index: Int,
        value: String,
        txSize: BigDecimal,
    ): ImmutableList<CustomFeeFieldUM> {
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
                        label = getFiatReference(
                            rate = feeCryptoCurrencyStatus.value.fiatRate,
                            value = newFeeAmount,
                            appCurrency = appCurrency,
                        ),
                    ),
                )
                set(index, this[index].copy(value = value))
            }
        }.toImmutableList()
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