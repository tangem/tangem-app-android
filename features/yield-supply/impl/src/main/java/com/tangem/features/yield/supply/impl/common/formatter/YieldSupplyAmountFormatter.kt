package com.tangem.features.yield.supply.impl.common.formatter

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.approximateAmount
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.utils.StringsSigns.DOT
import java.math.BigDecimal

internal class YieldSupplyAmountFormatter(
    private val feeCryptoCurrency: CryptoCurrency,
    private val appCurrency: AppCurrency,
) {

    operator fun invoke(feeValue: BigDecimal, fiatRate: BigDecimal?, showCrypto: Boolean = true): TextReference {
        val cryptoFee = feeValue.format { crypto(feeCryptoCurrency) }
        val fiatFeeValue = fiatRate?.let(feeValue::multiply)
        val fiatFee = if (showCrypto) {
            fiatFeeValue.format {
                fiat(appCurrency.code, appCurrency.symbol)
                    .approximateAmount()
            }
        } else {
            fiatFeeValue.format {
                fiat(appCurrency.code, appCurrency.symbol)
            }
        }

        return if (showCrypto) {
            stringReference("$cryptoFee $DOT $fiatFee")
        } else {
            stringReference(fiatFee)
        }
    }
}