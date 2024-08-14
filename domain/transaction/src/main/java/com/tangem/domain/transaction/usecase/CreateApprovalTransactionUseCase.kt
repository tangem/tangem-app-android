package com.tangem.domain.transaction.usecase

import arrow.core.Either
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.utils.convertToAmount
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.wallets.models.UserWalletId
import java.math.BigDecimal

class CreateApprovalTransactionUseCase(
    private val transactionRepository: TransactionRepository,
) {

    @Suppress("LongParameterList")
    suspend operator fun invoke(
        cryptoCurrency: CryptoCurrency.Token,
        userWalletId: UserWalletId,
        amount: BigDecimal,
        fee: Fee,
        contractAddress: String,
        spenderAddress: String,
        hash: String? = null,
    ) = Either.catch {
        transactionRepository.createApprovalTransaction(
            amount = amount.convertToAmount(cryptoCurrency),
            contractAddress = contractAddress,
            spenderAddress = spenderAddress,
            userWalletId = userWalletId,
            network = cryptoCurrency.network,
            fee = fee,
            hash = hash,
        )
    }
}
