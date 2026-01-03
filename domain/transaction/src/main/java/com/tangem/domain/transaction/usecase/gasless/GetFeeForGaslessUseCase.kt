package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.left
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.GetFeeError.GaslessError

class GetFeeForGaslessUseCase {

    operator fun invoke(
        userWalletId: UserWalletId,
        network: Network,
        transactionData: TransactionData,
    ): Either<GetFeeError, TransactionFee> {
        return GaslessError.NetworkIsNotSupported.left()
    }
}