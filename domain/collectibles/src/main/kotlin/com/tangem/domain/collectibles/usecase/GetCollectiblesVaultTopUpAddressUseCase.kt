package com.tangem.domain.collectibles.usecase

import arrow.core.Either
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.models.wallet.UserWallet
import javax.inject.Inject

class GetCollectiblesVaultTopUpAddressUseCase @Inject constructor(
    private val gachaVaultRepository: GachaVaultRepository,
) {

    suspend operator fun invoke(userWallet: UserWallet): Either<CollectiblesAccountError, String> =
        gachaVaultRepository.getVaultTopUpAddress(userWallet)
}