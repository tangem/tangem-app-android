package com.tangem.domain.tokens.model.warnings

import com.tangem.domain.tokens.model.blockchains.UtxoAmountLimit
import java.math.BigDecimal

data class CryptoCurrencyCheck(
    val dustValue: BigDecimal?,
    val reserveAmount: BigDecimal?,
    val minimumSendAmount: BigDecimal?,
    val existentialDeposit: BigDecimal?,
    val utxoAmountLimit: UtxoAmountLimit?,
    val isAccountFunded: Boolean,
    val rentWarning: CryptoCurrencyWarning.Rent?,
)