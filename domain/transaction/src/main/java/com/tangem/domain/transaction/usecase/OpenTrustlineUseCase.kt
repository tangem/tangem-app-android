package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.error.OpenTrustlineError
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId

class OpenTrustlineUseCase(
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val walletManagersFacade: WalletManagersFacade,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
    ): Either<OpenTrustlineError, Unit> {
        return either {
            val signer = cardSdkConfigRepository.getCommonSigner(
                cardId = null,
                twinKey = null, // use null here because no assets support for Twin cards
            )

            catch(
                block = {
                    when (val result = walletManagersFacade.fulfillRequirements(userWalletId, currency, signer)) {
                        is SimpleResult.Failure -> when (val error = result.error) {
                            is BlockchainSdkError.Stellar.MinReserveRequired ->
                                raise(OpenTrustlineError.NotEnoughCoin(error.amount, result.error.customMessage))
                            is BlockchainSdkError.Xrp.MinReserveRequired ->
                                raise(OpenTrustlineError.NotEnoughCoin(error.amount, result.error.customMessage))
                            else -> raise(OpenTrustlineError.SomeError(result.error.customMessage))
                        }
                        SimpleResult.Success -> Unit
                    }
                },
                catch = { error -> raise(OpenTrustlineError.SomeError(error.message)) },
            )
        }
    }
}