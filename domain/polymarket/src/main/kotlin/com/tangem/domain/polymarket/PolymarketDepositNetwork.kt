package com.tangem.domain.polymarket

import com.tangem.blockchain.common.Blockchain

/**
 * The chain Predictions settles on.
 *
 * Everything the feature needs to name that chain — the backend network id a wallet's currencies are matched
 * against, and the coin that adding the chain to a portfolio means adding — is derived from this one value,
 * so no part of the feature carries a chain literal of its own.
 */
val PolymarketDepositBlockchain: Blockchain = Blockchain.Polygon