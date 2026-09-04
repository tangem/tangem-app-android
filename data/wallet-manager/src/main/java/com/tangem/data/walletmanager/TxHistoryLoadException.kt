package com.tangem.data.walletmanager

import com.tangem.blockchain.common.BlockchainError
import com.tangem.domain.models.currency.CryptoCurrency

/**
 * A blockchain SDK failure while loading a transaction history page. Keeps the SDK error as the [cause] so that the
 * real reason (an HTTP error, a swallowed cancellation, a parsing failure) survives into the logs instead of the bare
 * error code the SDK exposes as its message.
 */
internal class TxHistoryLoadException(
    currency: CryptoCurrency,
    error: BlockchainError,
) : IllegalStateException(
    "Unable to load tx history of ${currency.symbol} in ${currency.network.name}: ${error.customMessage}",
    error,
)