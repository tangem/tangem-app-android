package com.tangem.domain.polymarket

import com.tangem.blockchain.common.Blockchain

/**
 * The chain Predictions settles on.
 *
 * Nothing in onboarding reads it any more: the owner key is derived on a hardened path that belongs to no
 * network, so a wallet needs nothing in its portfolio to be onboarded. It is the withdraw flow that will name
 * a chain, and it should derive it from this one value rather than carry a literal of its own.
 */
val PolymarketDepositBlockchain: Blockchain = Blockchain.Polygon