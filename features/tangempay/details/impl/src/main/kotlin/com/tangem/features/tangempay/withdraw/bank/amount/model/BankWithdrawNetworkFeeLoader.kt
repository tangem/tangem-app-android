package com.tangem.features.tangempay.withdraw.bank.amount.model

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.right
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountCryptoCurrencyStatusUseCase
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.CreateTransferTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForTokenUseCase
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
import java.math.BigDecimal
import javax.inject.Inject

internal class BankWithdrawNetworkFeeLoader @Inject constructor(
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getMoneyAccountCryptoCurrencyStatusUseCase: GetMoneyAccountCryptoCurrencyStatusUseCase,
    private val createTransferTransactionUseCase: CreateTransferTransactionUseCase,
    private val getFeeForTokenUseCase: GetFeeForTokenUseCase,
) {

    suspend fun load(account: BankWithdrawSourceAccount, token: CryptoCurrency.Token): Either<GetFeeError, BigDecimal> =
        when (account) {
            is BankWithdrawSourceAccount.Payment -> BigDecimal.ZERO.right()
            is BankWithdrawSourceAccount.Money -> loadAccountTransferFee(account.userWalletId, token)
        }

    private suspend fun loadAccountTransferFee(
        userWalletId: UserWalletId,
        token: CryptoCurrency.Token,
    ): Either<GetFeeError, BigDecimal> = either {
        val userWallet = getUserWalletUseCase(userWalletId).mapLeft { GetFeeError.UnknownError }.bind()
        val status = ensureNotNull(
            getMoneyAccountCryptoCurrencyStatusUseCase.invokeSync(userWalletId, token).getOrNull()?.second,
        ) { GetFeeError.UnknownError }
        val balance = ensureNotNull(status.value.amount) { GetFeeError.UnknownError }
        val fiatRate = ensureNotNull(status.value.fiatRate) { GetFeeError.UnknownError }
        val accountAddress = ensureNotNull(status.value.networkAddress?.defaultAddress?.value) {
            GetFeeError.UnknownError
        }
        val transaction = createTransferTransactionUseCase(
            amount = balance.convertToSdkAmount(status),
            memo = null,
            destination = accountAddress,
            userWalletId = userWalletId,
            network = token.network,
        ).mapLeft(GetFeeError::DataError).bind()
        val fee = getFeeForTokenUseCase(
            userWallet = userWallet,
            token = token,
            transactionData = transaction,
            sentAmount = balance,
        ).bind()
        val feeAmount = ensureNotNull(fee.transactionFee.normal.amount.value) { GetFeeError.UnknownError }
        feeAmount.multiply(fiatRate)
    }
}