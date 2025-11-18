package com.tangem.features.yield.supply.impl.subcomponents.active.model.transformers

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.subcomponents.active.entity.YieldSupplyActiveContentUM
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal

/**
 * Computes and sets minimum amount (fiat) and fee description note
 */
internal class YieldSupplyActiveMinAmountTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val appCurrency: AppCurrency,
    private val minAmount: BigDecimal,
) : Transformer<YieldSupplyActiveContentUM> {

    override fun transform(prevState: YieldSupplyActiveContentUM): YieldSupplyActiveContentUM {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val tokenFiatRate = cryptoCurrencyStatus.value.fiatRate

        val minAmountCryptoText = minAmount.format { crypto(cryptoCurrency) }
        val minAmountFiat = tokenFiatRate?.let(minAmount::multiply)
        val minAmountFiatText = minAmountFiat.format { fiat(appCurrency.code, appCurrency.symbol) }

        val minFeeNoteValue = resourceReference(
            id = R.string.yield_module_fee_policy_sheet_min_amount_note,
            formatArgs = wrappedList(
                minAmountFiatText,
                minAmountCryptoText,
            ),
        )

        return prevState.copy(
            minAmount = stringReference(minAmountFiatText),
            minFeeDescription = minFeeNoteValue,
        )
    }
}