package com.tangem.domain.transaction.usecase.gasless

import com.tangem.domain.models.network.Network
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.lib.crypto.BlockchainUtils.isEvm

/**
 * The service's network list is remote configuration, while the EIP-712/7702 machinery works only on an
 * EVM chain — so the chain check is ours and cannot be turned off from the backend.
 */
internal fun CurrencyChecksRepository.isEvmGaslessNetwork(network: Network): Boolean =
    isEvm(network.rawId) && isNetworkSupportedForGaslessTx(network)