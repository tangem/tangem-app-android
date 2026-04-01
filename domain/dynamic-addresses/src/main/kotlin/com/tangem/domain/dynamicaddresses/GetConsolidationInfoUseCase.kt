package com.tangem.domain.dynamicaddresses

import arrow.core.Either
import com.tangem.domain.dynamicaddresses.model.ConsolidationInfo
import com.tangem.domain.dynamicaddresses.repository.ConsolidationRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

class GetConsolidationInfoUseCase(
    private val consolidationRepository: ConsolidationRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network): Either<Throwable, ConsolidationInfo> {
        return consolidationRepository.getConsolidationInfo(userWalletId, network)
    }
}