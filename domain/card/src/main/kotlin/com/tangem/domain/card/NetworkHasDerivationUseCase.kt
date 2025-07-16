package com.tangem.domain.card

import arrow.core.Either
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.common.util.hasDerivation
import com.tangem.domain.models.network.Network
import com.tangem.domain.wallets.models.UserWallet

class NetworkHasDerivationUseCase {

    operator fun invoke(userWallet: UserWallet, network: Network): Either<Throwable, Boolean> = Either.catch {
        when (userWallet) {
            is UserWallet.Cold -> {
                val blockchain = network.toBlockchain()
                val derivationPath = network.derivationPath.value
                derivationPath != null && userWallet.scanResponse.hasDerivation(blockchain, derivationPath)
            }
            is UserWallet.Hot -> true
        }
    }
}