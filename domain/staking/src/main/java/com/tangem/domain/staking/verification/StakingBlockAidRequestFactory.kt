package com.tangem.domain.staking.verification

import com.domain.blockaid.models.transaction.TransactionData
import com.tangem.domain.models.staking.NetworkType

/**
 * Builds a Blockaid [TransactionData] from a StakeKit unsigned transaction for Blockaid-supported
 * networks (EVM: Ethereum/Polygon/BSC, Solana). Handles chain-name mapping and per-chain format
 * conversion (EVM JSON object -> JSON-RPC params array; Solana hex -> base64).
 */
interface StakingBlockAidRequestFactory {

    /** @throws IllegalArgumentException if [network] is not a Blockaid-supported staking network. */
    fun create(network: NetworkType, accountAddress: String, unsignedTransaction: String): TransactionData
}