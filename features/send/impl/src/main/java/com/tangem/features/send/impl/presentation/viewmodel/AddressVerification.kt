package com.tangem.features.send.impl.presentation.viewmodel

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.Address
import com.tangem.domain.tokens.model.CryptoCurrency

internal fun verifyAddress(address: String, cryptoCurrency: CryptoCurrency?): Boolean {
    if (address.isEmpty()) return true
    val blockchain = cryptoCurrency?.let {
        Blockchain.fromId(cryptoCurrency.id.rawNetworkId)
    } ?: return false

    return blockchain.validateAddress(address)
}

internal fun isNotAddressInWallet(walletAddresses: Set<Address>, address: String): Boolean {
    return walletAddresses.all { it.value != address }
}