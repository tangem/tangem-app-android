package com.tangem.domain.card

import arrow.core.Either
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.util.hasDerivation
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.scan.ScanResponse

class NetworkHasDerivationUseCase {

    operator fun invoke(scanResponse: ScanResponse, network: Network): Either<Throwable, Boolean> {
        val blockchain = Blockchain.fromId(network.rawId)
        val derivationPath = network.derivationPath.value
        return Either.catch { derivationPath != null && scanResponse.hasDerivation(blockchain, derivationPath) }
    }
}