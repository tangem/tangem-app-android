package com.tangem.domain.wallets.repository

import com.tangem.domain.models.network.Network

interface HotDerivationsRepository {

    fun getAllSupportedNetworks(): Set<Network>
}