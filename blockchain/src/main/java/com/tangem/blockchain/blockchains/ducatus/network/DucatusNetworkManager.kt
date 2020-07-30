package com.tangem.blockchain.blockchains.ducatus.network

import com.tangem.blockchain.blockchains.ducatus.network.bitcore.BitcoreApi
import com.tangem.blockchain.blockchains.ducatus.network.bitcore.BitcoreProvider
import com.tangem.blockchain.network.API_DUCATUS
import com.tangem.blockchain.network.createRetrofitInstance

class DucatusNetworkManager() : BitcoreProvider(createRetrofitInstance(API_DUCATUS).create(BitcoreApi::class.java))