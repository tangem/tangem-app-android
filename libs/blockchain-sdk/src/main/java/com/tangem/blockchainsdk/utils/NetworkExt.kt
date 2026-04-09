package com.tangem.blockchainsdk.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network

/** Converts [Network] to [Blockchain] */
fun Network.toBlockchain(): Blockchain = id.toBlockchain()

/** Converts [Network.ID] to [Blockchain] */
fun Network.ID.toBlockchain(): Blockchain = rawId.toBlockchain()

/** Converts [Network.RawID] to [Blockchain] */
fun Network.RawID.toBlockchain(): Blockchain = value.toBlockchain()

fun CryptoCurrency.ID.toBlockchain(): Blockchain = rawNetworkId.toBlockchain()

private fun String.toBlockchain(): Blockchain = Blockchain.fromNetworkId(this) ?: Blockchain.Unknown