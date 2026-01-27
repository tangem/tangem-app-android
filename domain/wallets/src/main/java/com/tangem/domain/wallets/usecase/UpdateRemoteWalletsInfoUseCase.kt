package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.notifications.models.ApplicationId
import com.tangem.domain.wallets.delegate.UserWalletsSyncDelegate
import com.tangem.domain.wallets.repository.WalletsRepository

class UpdateRemoteWalletsInfoUseCase(
    private val walletsRepository: WalletsRepository,
    private val userWalletsSyncDelegate: UserWalletsSyncDelegate,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val generateWalletNameUseCase: GenerateWalletNameUseCase,
) {

    suspend operator fun invoke(applicationId: ApplicationId): Either<Throwable, Unit> = Either.catch {
        val userWalletsMap = userWalletsListRepository.userWallets.value?.associateBy { it.walletId }.orEmpty()
        val remoteData = walletsRepository.getWalletsInfo(applicationId.value)

        val walletsInfo = remoteData.map { walletRemoteInfo ->
            if (walletRemoteInfo.name.isBlank()) {
                walletRemoteInfo.copy(name = generateName(walletRemoteInfo.walletId, userWalletsMap))
            } else {
                walletRemoteInfo
            }
        }.filter { it.name.isNotBlank() }

        userWalletsSyncDelegate.syncWallets(walletsInfo)
    }

    private fun generateName(userWalletId: UserWalletId, userWalletsMap: Map<UserWalletId, UserWallet>): String {
        val userWallet = userWalletsMap[userWalletId] ?: return ""
        return when (userWallet) {
            is UserWallet.Hot -> generateWalletNameUseCase.invokeForHot()
            is UserWallet.Cold -> generateWalletNameUseCase(
                userWallet.scanResponse.productType,
                card = userWallet.scanResponse.card,
                isStartToCoin = userWallet.scanResponse.cardTypesResolver.isStart2Coin(),
            )
        }
    }
}