package com.tangem.domain.transaction.usecase

import arrow.core.Either
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.wallets.models.UserWalletId
import java.math.BigDecimal

/**
 * Use case to create and get approval transaction
 */
class CreateApprovalTransactionUseCase(
    private val transactionRepository: TransactionRepository,
) {

    @Suppress("LongParameterList")
    suspend operator fun invoke(
        cryptoCurrency: CryptoCurrency.Token,
        userWalletId: UserWalletId,
        amount: BigDecimal?,
        fee: Fee?,
        contractAddress: String,
        spenderAddress: String,
    ) = Either.catch {
        transactionRepository.createApprovalTransaction(
            amount = BigDecimal.ZERO.convertToSdkAmount(cryptoCurrency),
            approvalAmount = amount?.convertToSdkAmount(cryptoCurrency),
            contractAddress = contractAddress,
            spenderAddress = spenderAddress,
            userWalletId = userWalletId,
            network = cryptoCurrency.network,
            fee = fee,
        )
    }

    @Suppress("LongParameterList")
    suspend operator fun invoke(
        cryptoCurrency: CryptoCurrency.Token,
        userWalletId: UserWalletId,
        amount: BigDecimal?,
        contractAddress: String,
        spenderAddress: String,
    ) = Either.catch {
        transactionRepository.createApprovalTransaction(
            amount = BigDecimal.ZERO.convertToSdkAmount(cryptoCurrency),
            approvalAmount = amount?.convertToSdkAmount(cryptoCurrency),
            contractAddress = contractAddress,
            spenderAddress = spenderAddress,
            userWalletId = userWalletId,
            network = cryptoCurrency.network,
            fee = null,
        )
    }
}