package com.tangem.common

/**
 * Interface for a service for managing Terminal keypair, used for Linked Terminal feature.
 * Its implementation Needs to be provided to [com.tangem.CardManager]
 * by calling [com.tangem.CardManager.setTerminalKeysService].
 * Default implementation is provided in tangem-sdk module: [TerminalKeysStorage].
 * Linked Terminal feature can be disabled manually by editing [com.tangem.Config].
 */
interface TerminalKeysService {
    fun getKeys(): KeyPair
}