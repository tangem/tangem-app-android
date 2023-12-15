package com.tangem.lib.crypto

import com.tangem.lib.crypto.models.Currency

// FIXME: Migration [REDACTED_JIRA]
@Deprecated(message = "Use DerivePublicKeysUseCase instead")
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

    /**
     * Makes derivation for [Currency] if it is missing and adds token to wallet
     */
    suspend fun deriveAndAddTokens(currency: Currency): String
}