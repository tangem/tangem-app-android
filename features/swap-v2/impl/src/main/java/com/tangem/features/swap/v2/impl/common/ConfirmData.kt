package com.tangem.features.swap.v2.impl.common

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import java.math.BigDecimal

internal data class ConfirmData(
    val enteredAmount: BigDecimal?,
    val reduceAmountBy: BigDecimal,
    val isIgnoreReduce: Boolean,
    val enteredDestination: String?,
    val fee: Fee?,
    val feeError: GetFeeError?,
    val fromCryptoCurrencyStatus: CryptoCurrencyStatus?,
    val toCryptoCurrencyStatus: CryptoCurrencyStatus?,
    val quote: SwapQuoteUM?,
    val rateType: ExpressRateType?,
)