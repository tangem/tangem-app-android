package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.error.IncompleteTransactionError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.error.parseWrappedError
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.models.wallet.UserWalletId

class RetryIncompleteTransactionUseCase(
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val walletManagersFacade: WalletManagersFacade,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
    ): Either<IncompleteTransactionError, Unit> {
        return either {
            val signer = cardSdkConfigRepository.getCommonSigner(
                cardId = null,
                twinKey = null, // use null here because no assets support for Twin cards
            )

            catch(
                block = {
                    when (val result = walletManagersFacade.fulfillRequirements(userWalletId, currency, signer)) {
                        is SimpleResult.Failure -> {
                            val error = result.error as? BlockchainSdkError
                            when (error) {
                                is BlockchainSdkError.WrappedTangemError -> parseWrappedError(error)
                                null -> SendTransactionError.UnknownError()
                                else -> SendTransactionError.BlockchainSdkError(
                                    code = error.code,
                                    message = error.customMessage,
                                )
                            }.let {
                                raise(IncompleteTransactionError.SendError(it))
                            }
                        }
                        SimpleResult.Success -> Unit
                    }
                },
                catch = { error -> IncompleteTransactionError.DataError(error.message) },
            )
        }
    }
}