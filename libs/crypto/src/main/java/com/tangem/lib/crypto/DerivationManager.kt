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
     * Checks that given [networkId] has derivations
     */
    fun hasDerivation(networkId: String): Boolean
}
