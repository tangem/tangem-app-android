package com.tangem.domain.assetsdiscovery.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.assetsdiscovery.repository.AssetsDiscoveryRepository

class AcknowledgeAssetsDiscoveryCompletionUseCase(
    private val assetsDiscoveryRepository: AssetsDiscoveryRepository,
) {

    operator fun invoke(userWalletId: UserWalletId) {
        assetsDiscoveryRepository.acknowledgeCompletion(userWalletId)
    }
}