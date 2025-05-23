package com.tangem.features.send.v2.common.utils

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fee
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.send.v2.impl.R
import com.tangem.utils.StringsSigns.COMA_SIGN

internal fun getTronTokenFeeSendingText(fee: Fee.Tron, fiatFee: String, fiatSending: TextReference): TextReference {
    val suffix = when {
        fee.remainingEnergy == 0L -> {
            resourceReference(
                R.string.send_summary_transaction_description_suffix_including,
                wrappedList(fiatFee),
            )
        }
        fee.feeEnergy <= fee.remainingEnergy -> {
            resourceReference(
                R.string.send_summary_transaction_description_suffix_fee_covered,
                wrappedList(fee.feeEnergy),
            )
        }
        else -> {
            resourceReference(
                R.string.send_summary_transaction_description_suffix_fee_reduced,
                wrappedList(fee.remainingEnergy),
            )
        }
    }
    val prefix = resourceReference(
        R.string.send_summary_transaction_description_prefix,
        wrappedList(fiatSending),
    )

    return combinedReference(prefix, stringReference("$COMA_SIGN "), suffix)
}

internal fun formatFooterFiatFee(
    amount: Amount?,
    isFeeConvertibleToFiat: Boolean,
    isFeeApproximate: Boolean,
    appCurrency: AppCurrency,
): String {
    return if (isFeeConvertibleToFiat) {
        amount?.value.format {
            fiat(
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
            )
        }
    } else {
        amount?.value.format {
            crypto(
                decimals = amount?.decimals ?: 0,
                symbol = amount?.currencySymbol.orEmpty(),
            ).fee(
                canBeLower = isFeeApproximate,
            )
        }
    }
}