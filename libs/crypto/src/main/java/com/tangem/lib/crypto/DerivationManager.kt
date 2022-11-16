package com.tangem.crypto

interface DerivationManager {

    fun deriveMissingBlockchains(networkId: String?)

    fun hasDerivation(): Boolean
}