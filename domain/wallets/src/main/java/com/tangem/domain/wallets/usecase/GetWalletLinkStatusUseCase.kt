package com.tangem.domain.wallets.usecase

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.registration.WalletDeviceLinkRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetWalletLinkStatusUseCase @Inject constructor(
    private val walletDeviceLinkRepository: WalletDeviceLinkRepository,
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    operator fun invoke(userWallet: UserWallet): Flow<Boolean> = walletDeviceLinkRepository.isLinked(userWallet)

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(userWalletId: UserWalletId): Flow<Boolean> = userWalletsListRepository.userWallets
        .map { userWallets -> userWallets?.firstOrNull { it.walletId == userWalletId } }
        .distinctUntilChanged()
        .flatMapLatest { userWallet -> if (userWallet == null) flowOf(true) else invoke(userWallet) }
        .distinctUntilChanged()
}