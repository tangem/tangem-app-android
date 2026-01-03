package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.left
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.error.GetFeeError

class GetFeeForTokenUseCase {

    operator fun invoke(token: CryptoCurrency, transactionData: TransactionData): Either<GetFeeError, TransactionFee> {
        return GetFeeError.UnknownError.left()
    }
}