package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.registration.WalletDeviceLinkRepository
import com.tangem.domain.wallets.registration.WalletLinkError
import javax.inject.Inject

class LinkWalletToDeviceUseCase @Inject constructor(
    private val walletDeviceLinkRepository: WalletDeviceLinkRepository,
) {

    suspend operator fun invoke(userWallet: UserWallet): Either<WalletLinkError, Unit> =
        walletDeviceLinkRepository.link(userWallet)
}