package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.error.IncompleteTransactionError
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.models.wallet.UserWalletId

class DismissIncompleteTransactionUseCase(
    private val walletManagersFacade: WalletManagersFacade,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
    ): Either<IncompleteTransactionError.DataError, Unit> {
        return either {
            catch(
                block = {
                    when (val result = walletManagersFacade.discardRequirements(userWalletId, currency)) {
                        is SimpleResult.Failure -> raise(
                            IncompleteTransactionError.DataError(result.error.customMessage),
                        )
                        SimpleResult.Success -> Unit
                    }
                },
                catch = { error -> IncompleteTransactionError.DataError(error.message) },
            )
        }
    }
}