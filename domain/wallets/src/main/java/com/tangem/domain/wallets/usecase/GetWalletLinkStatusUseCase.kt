package com.tangem.domain.wallets.usecase

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.registration.WalletDeviceLinkRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetWalletLinkStatusUseCase @Inject constructor(
    private val walletDeviceLinkRepository: WalletDeviceLinkRepository,
) {

    operator fun invoke(userWallet: UserWallet): Flow<Boolean> = walletDeviceLinkRepository.isLinked(userWallet)
}