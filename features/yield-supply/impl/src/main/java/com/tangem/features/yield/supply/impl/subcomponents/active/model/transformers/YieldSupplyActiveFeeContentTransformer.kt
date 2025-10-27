package com.tangem.features.yield.supply.impl.subcomponents.active.model.transformers

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.subcomponents.active.entity.YieldSupplyActiveContentUM
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal

/**
 * Computes fee description and current fee values for Active screen based on token rates.
 */
internal class YieldSupplyActiveFeeContentTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val appCurrency: AppCurrency,
    private val feeValue: BigDecimal,
    private val maxNetworkFee: BigDecimal,
) : Transformer<YieldSupplyActiveContentUM> {

    override fun transform(prevState: YieldSupplyActiveContentUM): YieldSupplyActiveContentUM {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val tokenFiatRate = cryptoCurrencyStatus.value.fiatRate

        val tokenCryptoFeeValueText = feeValue.format { crypto(cryptoCurrency) }
        val tokenFiatFee = tokenFiatRate?.let(feeValue::multiply)
        val tokenFiatFeeValueText = tokenFiatFee.format { fiat(appCurrency.code, appCurrency.symbol) }

        val maxFeeCryptoValueText = maxNetworkFee.format { crypto(cryptoCurrency) }
        val maxFiatFee = tokenFiatRate?.let { rate -> maxNetworkFee.multiply(rate) }
        val maxFiatFeeValueText = maxFiatFee.format { fiat(appCurrency.code, appCurrency.symbol) }

        val feeNoteValue: TextReference = resourceReference(
            id = R.string.yield_module_fee_policy_sheet_fee_note,
            formatArgs = wrappedList(
                stringReference(tokenFiatFeeValueText),
                stringReference(tokenCryptoFeeValueText),
                stringReference(maxFiatFeeValueText),
                stringReference(maxFeeCryptoValueText),
            ),
        )

        val isHighFee = feeValue > maxNetworkFee

        return prevState.copy(
            currentFee = stringReference(tokenFiatFeeValueText),
            feeDescription = feeNoteValue,
            isHighFee = isHighFee,
        )
    }
}