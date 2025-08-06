package com.tangem.features.send.v2.subcomponents.fee.model.converters.custom.ethereum

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.amountScreen.utils.getFiatReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.v2.api.entity.CustomFeeFieldUM
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.subcomponents.fee.model.checkExceedBalance
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal class EthereumCustomFeeConverter(
    private val onCustomFeeValueChange: (Int, String) -> Unit,
    private val onNextClick: (() -> Unit)?,
    private val appCurrency: AppCurrency,
    feeCryptoCurrencyStatus: CryptoCurrencyStatus,
) : BaseEthereumCustomFeeConverter<Fee.Ethereum> {

    private val currencyStatus = feeCryptoCurrencyStatus.value

    private val legacyFeeConverter = EthereumLegacyCustomFeeConverter(
        onCustomFeeValueChange = onCustomFeeValueChange,
        appCurrency = appCurrency,
        feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
    )

    private val eipFeeConverter = EthereumEIPCustomFeeConverter(
        onCustomFeeValueChange = onCustomFeeValueChange,
        appCurrency = appCurrency,
        feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
    )

    override fun convert(value: Fee.Ethereum): ImmutableList<CustomFeeFieldUM> {
        return buildList {
            convertFeeValue(value).let(::add)

            when (value) {
                is Fee.Ethereum.EIP1559 -> eipFeeConverter.convert(value)
                is Fee.Ethereum.Legacy -> legacyFeeConverter.convert(value)
            }.let(::addAll)

            convertGasLimitValue(value).let(::add)
        }
            .toImmutableList()
    }

    override fun convertBack(normalFee: Fee.Ethereum, value: ImmutableList<CustomFeeFieldUM>): Fee.Ethereum {
        return when (normalFee) {
            is Fee.Ethereum.EIP1559 -> eipFeeConverter.convertBack(normalFee = normalFee, value = value)
            is Fee.Ethereum.Legacy -> legacyFeeConverter.convertBack(normalFee = normalFee, value = value)
        }
    }

    override fun getGasLimitIndex(feeValue: Fee.Ethereum): Int {
        return when (feeValue) {
            is Fee.Ethereum.EIP1559 -> eipFeeConverter.getGasLimitIndex(feeValue)
            is Fee.Ethereum.Legacy -> legacyFeeConverter.getGasLimitIndex(feeValue)
        }
    }

    override fun onValueChange(
        feeValue: Fee.Ethereum,
        customValues: ImmutableList<CustomFeeFieldUM>,
        index: Int,
        value: String,
    ): ImmutableList<CustomFeeFieldUM> {
        return when (feeValue) {
            is Fee.Ethereum.EIP1559 -> eipFeeConverter.onValueChange(feeValue, customValues, index, value)
            is Fee.Ethereum.Legacy -> legacyFeeConverter.onValueChange(feeValue, customValues, index, value)
        }
    }

    private fun convertFeeValue(value: Fee.Ethereum): CustomFeeFieldUM {
        val feeValue = value.amount.value

        return CustomFeeFieldUM(
            value = feeValue?.parseBigDecimal(value.amount.decimals).orEmpty(),
            decimals = value.amount.decimals,
            symbol = value.amount.currencySymbol,
            onValueChange = { onCustomFeeValueChange(FEE_AMOUNT, it) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Number),
            title = resourceReference(R.string.send_max_fee),
            footer = resourceReference(R.string.send_custom_amount_fee_footer),
            label = getFiatReference(
                rate = currencyStatus.fiatRate,
                value = feeValue,
                appCurrency = appCurrency,
            ),
            keyboardActions = KeyboardActions(),
        )
    }

    private fun convertGasLimitValue(value: Fee.Ethereum): CustomFeeFieldUM {
        val isExceedBalance = checkExceedBalance(feeBalance = currencyStatus.amount, feeAmount = value.amount.value)

        return CustomFeeFieldUM(
            value = value.gasLimit.toString(),
            decimals = GAS_DECIMALS,
            symbol = "",
            title = resourceReference(R.string.send_gas_limit),
            footer = resourceReference(R.string.send_gas_limit_footer),
            onValueChange = { onCustomFeeValueChange(getGasLimitIndex(value), it) },
            keyboardOptions = KeyboardOptions(
                imeAction = if (isExceedBalance) ImeAction.None else ImeAction.Done,
                keyboardType = KeyboardType.Number,
            ),
            keyboardActions = KeyboardActions(
                onDone = if (onNextClick != null) {
                    { onNextClick() }
                } else {
                    null
                },
            ),
        )
    }

    companion object {
        const val ETHEREUM_GAS_UNIT = "GWEI"
        const val GIGA_DECIMALS = 9
        const val GAS_DECIMALS = 0
        const val FEE_AMOUNT = 0
    }
}