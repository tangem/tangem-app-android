package com.tangem.common

import com.tangem.KeyPair

/**
 * Interface for a service for managing Terminal keypair, used for Linked Terminal feature.
 * Its implementation Needs to be provided to [com.tangem.TangemSdk]
 * by calling [com.tangem.TangemSdk.setTerminalKeysService].
 * Default implementation is provided in tangem-sdk module: [TerminalKeysStorage].
 * Linked Terminal feature can be disabled manually by editing [com.tangem.Config].
 */
interface TerminalKeysService {
    fun getKeys(): KeyPair
}