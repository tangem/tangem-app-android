package com.tangem.domain.staking.verification

import com.tangem.domain.models.staking.NetworkType

/**
 * Recognizes whether a raw unsigned StakeKit transaction is a known staking operation,
 * for networks not covered by Blockaid (Tron, Cosmos, Cardano). Pure parsing, no I/O.
 */
interface StakingTransactionRecognizer {

    /**
     * @return true if [unsignedTransaction] matches a known staking signature for [network];
     * false if it does not match OR cannot be parsed (caller treats false as fail-closed).
     */
    fun isRecognizedStakingTransaction(network: NetworkType, unsignedTransaction: String): Boolean
}