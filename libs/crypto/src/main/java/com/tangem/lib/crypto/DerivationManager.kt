package com.tangem.lib.crypto

interface DerivationManager {

    suspend fun deriveMissingBlockchains(networkId: String)

    fun hasDerivation(networkId: String): Boolean
}