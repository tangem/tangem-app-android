package com.tangem.domain.managetokens

import arrow.core.Either
import com.tangem.domain.managetokens.model.AddCustomTokenForm
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.wallets.models.UserWalletId

class CreateCurrencyUseCase(
    private val repository: CustomTokensRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        formValues: AddCustomTokenForm.Validated.All?,
    ): Either<Throwable, CryptoCurrency> = Either.catch {
        if (formValues == null) {
            repository.createCoin(userWalletId, networkId, derivationPath)
        } else {
            repository.createCustomToken(userWalletId, networkId, derivationPath, formValues)
        }
    }
}