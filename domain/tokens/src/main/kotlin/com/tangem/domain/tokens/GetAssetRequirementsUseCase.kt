package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.models.AssetRequirementsCondition
import com.tangem.domain.walletmanager.WalletManagersFacade

class GetAssetRequirementsUseCase(
    private val walletManagersFacade: WalletManagersFacade,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
    ): Either<Throwable, AssetRequirementsCondition?> {
        return Either.Companion.catch {
            walletManagersFacade.getAssetRequirements(userWalletId, currency)
        }
    }
}