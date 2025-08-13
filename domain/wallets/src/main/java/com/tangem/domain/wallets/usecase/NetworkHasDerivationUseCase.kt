package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.extension.hasDerivation

class NetworkHasDerivationUseCase {

    operator fun invoke(userWallet: UserWallet, network: Network): Either<Throwable, Boolean> = Either.catch {
        val blockchain = network.toBlockchain()
        val derivationPath = network.derivationPath.value
        derivationPath != null && userWallet.hasDerivation(blockchain, derivationPath)
    }
}