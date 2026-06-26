package com.tangem.features.send.subcomponents.fee.model.converters.custom.ethereum

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.loadedStatus
import java.math.BigDecimal

internal const val ETH_DECIMALS = 18

/** Index of the read-only fee-amount field, shared by every Ethereum custom-fee field layout. */
internal const val FEE_AMOUNT_INDEX = 0

internal fun ethAmount(value: BigDecimal?) = Amount(currencySymbol = "ETH", value = value, decimals = ETH_DECIMALS)

/** Loaded ETH status with a 1 ETH balance — the shared fixture for the Ethereum custom-fee converter tests. */
internal fun ethFeeStatus(): CryptoCurrencyStatus = loadedStatus(
    currency = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum),
    fiatRate = BigDecimal("2000"),
)