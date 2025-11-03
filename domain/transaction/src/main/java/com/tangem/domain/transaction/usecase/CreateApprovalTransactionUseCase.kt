package com.tangem.domain.transaction.usecase

import arrow.core.Either
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.utils.convertToSdkAmount
import java.math.BigDecimal

/**
 * Use case to create and get approval transaction
 */
class CreateApprovalTransactionUseCase(
    private val transactionRepository: TransactionRepository,
) {

    @Suppress("LongParameterList")
    suspend operator fun invoke(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        userWalletId: UserWalletId,
        amount: BigDecimal?,
        fee: Fee?,
        contractAddress: String,
        spenderAddress: String,
    ) = Either.catch {
        transactionRepository.createApprovalTransaction(
            amount = BigDecimal.ZERO.convertToSdkAmount(cryptoCurrencyStatus),
            approvalAmount = amount?.convertToSdkAmount(cryptoCurrencyStatus),
            contractAddress = contractAddress,
            spenderAddress = spenderAddress,
            userWalletId = userWalletId,
            network = cryptoCurrencyStatus.currency.network,
            fee = fee,
        )
    }

    @Suppress("LongParameterList")
    suspend operator fun invoke(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        userWalletId: UserWalletId,
        amount: BigDecimal?,
        contractAddress: String,
        spenderAddress: String,
    ) = Either.catch {
        transactionRepository.createApprovalTransaction(
            amount = BigDecimal.ZERO.convertToSdkAmount(cryptoCurrencyStatus),
            approvalAmount = amount?.convertToSdkAmount(cryptoCurrencyStatus),
            contractAddress = contractAddress,
            spenderAddress = spenderAddress,
            userWalletId = userWalletId,
            network = cryptoCurrencyStatus.currency.network,
            fee = null,
        )
    }
}