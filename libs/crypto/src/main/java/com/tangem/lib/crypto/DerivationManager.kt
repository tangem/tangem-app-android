package com.tangem.lib.crypto

import com.tangem.lib.crypto.models.Currency

interface DerivationManager {

    /**
     * Derives missing blockchain and returns flag true if did it successfully
     *
     * @param currency to derive (Native token or not)
     */
    suspend fun deriveMissingBlockchains(currency: Currency): Boolean

    /**
     * Returns derivationPath or null for blockchain with given networkId
     *
     * @param networkId
     */
    fun getDerivationPathForBlockchain(networkId: String): String?

    /**
     * Checks that given [networkId] has derivations for [derivationPath]
     */
    fun hasDerivation(networkId: String, derivationPath: String): Boolean
}
