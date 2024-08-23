package com.tangem.domain.managetokens

import arrow.core.Either
import com.tangem.domain.managetokens.model.AddCustomTokenForm
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network

class CreateCurrencyUseCase(
    private val repository: CustomTokensRepository,
) {

    suspend operator fun invoke(
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        formValues: AddCustomTokenForm.Validated.All?,
    ): Either<Throwable, CryptoCurrency> = Either.catch {
        if (formValues == null) {
            repository.createCoin(networkId, derivationPath)
        } else {
            repository.createCustomToken(networkId, derivationPath, formValues)
        }
    }
}