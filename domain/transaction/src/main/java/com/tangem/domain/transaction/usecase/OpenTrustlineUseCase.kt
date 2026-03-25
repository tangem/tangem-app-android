package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.OpenTrustlineError
import com.tangem.domain.transaction.error.parseWrappedError
import com.tangem.domain.walletmanager.WalletManagersFacade

class OpenTrustlineUseCase(
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val getHotTransactionSigner: (UserWallet.Hot) -> TransactionSigner,
) {

    suspend operator fun invoke(userWallet: UserWallet, currency: CryptoCurrency): Either<OpenTrustlineError, Unit> {
        return either {
            val signer = createSigner(userWallet)

            catch(
                block = {
                    when (val result = walletManagersFacade.fulfillRequirements(
                        userWallet.walletId,
                        currency,
                        signer,
                    )) {
                        is SimpleResult.Failure -> when (val error = result.error) {
                            is BlockchainSdkError.Stellar.MinReserveRequired ->
                                raise(
                                    OpenTrustlineError.NotEnoughCoin(
                                        amount = error.amount,
                                        symbol = error.symbol,
                                        message = result.error.customMessage,
                                    ),
                                )
                            is BlockchainSdkError.Xrp.MinReserveRequired ->
                                raise(
                                    OpenTrustlineError.NotEnoughCoin(
                                        amount = error.amount,
                                        symbol = error.symbol,
                                        message = result.error.customMessage,
                                    ),
                                )
                            is BlockchainSdkError.WrappedTangemError ->
                                raise(OpenTrustlineError.SendError(parseWrappedError(error)))
                            else -> raise(OpenTrustlineError.UnknownError(result.error.customMessage))
                        }
                        SimpleResult.Success -> Unit
                    }
                },
                catch = { error -> raise(OpenTrustlineError.UnknownError(error.message)) },
            )
        }
    }

    private fun createSigner(userWallet: UserWallet): TransactionSigner {
        return when (userWallet) {
            is UserWallet.Hot -> getHotTransactionSigner(userWallet)
            is UserWallet.Cold -> getColdSigner()
        }
    }

    private fun getColdSigner(): TransactionSigner {
        return cardSdkConfigRepository.getCommonSigner(
            cardId = null,
            twinKey = null, // use null here because no assets support for Twin cards
        )
    }
}