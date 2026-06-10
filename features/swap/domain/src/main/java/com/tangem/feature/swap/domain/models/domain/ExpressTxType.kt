package com.tangem.feature.swap.domain.models.domain

/**
 * Type of transaction the express provider expects the app to execute, reported per quote.
 *
 * - [SWAP] — sign and broadcast a provider-built transaction (e.g. EVM smart-contract call);
 *   may require ERC-20 allowance.
 * - [SEND] — plain native transfer to a provider-supplied address; routes to the CEX-style flow.
 */
enum class ExpressTxType {
    SWAP,
    SEND,
}