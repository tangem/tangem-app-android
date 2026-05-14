package com.tangem.domain.assetsdiscovery.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.assetsdiscovery.model.AssetsDiscoveryProgress
import com.tangem.domain.assetsdiscovery.repository.AssetsDiscoveryRepository
import kotlinx.coroutines.flow.Flow

class ObserveAssetsDiscoveryUseCase(
    private val assetsDiscoveryRepository: AssetsDiscoveryRepository,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<AssetsDiscoveryProgress> {
        return assetsDiscoveryRepository.observeDiscoveryProgress(userWalletId)
    }
}