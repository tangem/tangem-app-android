package com.tangem.domain.managetokens

import arrow.core.Either
import com.tangem.domain.managetokens.model.AddCustomTokenForm
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Creates cryptocurrency from manage token
 */
class CreateCryptoCurrencyUseCase(
    private val customTokensRepository: CustomTokensRepository,
) {

    /**
     * Creates currency from default crypto currency
     *
     * @param token supported crypto currency info
     * @param userWalletId selected user wallet id
     */
    suspend operator fun invoke(
        token: ManagedCryptoCurrency.Token,
        userWalletId: UserWalletId,
    ): Either<Throwable, List<CryptoCurrency>> = Either.catch {
        token.availableNetworks
            .map { sourceNetwork ->
                when (sourceNetwork) {
                    is ManagedCryptoCurrency.SourceNetwork.Default -> customTokensRepository.createToken(
                        managedCryptoCurrency = token,
                        sourceNetwork = sourceNetwork,
                        rawId = CryptoCurrency.RawID(token.id.value),
                    )
                    is ManagedCryptoCurrency.SourceNetwork.Main -> customTokensRepository.createCoin(
                        userWalletId = userWalletId,
                        networkId = sourceNetwork.id,
                        derivationPath = sourceNetwork.network.derivationPath,
                    )
                }
            }
    }

    /**
     * Creates currency from custom crypto currency
     *
     * @param userWalletId selected user wallet id
     * @param networkId currency network id
     * @param derivationPath custom derivation path
     * @param formValues token info (e.i. name, contract address, symbol, decimals)
     */
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        formValues: AddCustomTokenForm.Validated.All?,
    ): Either<Throwable, CryptoCurrency> = Either.catch {
        if (formValues == null) {
            customTokensRepository.createCoin(userWalletId, networkId, derivationPath)
        } else {
            customTokensRepository.createCustomToken(userWalletId, networkId, derivationPath, formValues)
        }
    }
}