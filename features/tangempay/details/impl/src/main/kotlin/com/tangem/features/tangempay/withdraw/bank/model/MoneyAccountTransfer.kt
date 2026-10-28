package com.tangem.features.tangempay.withdraw.bank.model

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountCryptoCurrencyStatusUseCase
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.CreateTransferTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.CreateAndSendGaslessTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForTokenUseCase
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.utils.logging.TangemLogger
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

internal class MoneyAccountTransfer @Inject constructor(
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getMoneyAccountCryptoCurrencyStatusUseCase: GetMoneyAccountCryptoCurrencyStatusUseCase,
    private val createTransferTransactionUseCase: CreateTransferTransactionUseCase,
    private val getFeeForTokenUseCase: GetFeeForTokenUseCase,
    private val createAndSendGaslessTransactionUseCase: CreateAndSendGaslessTransactionUseCase,
) {

    suspend fun estimateFee(userWalletId: UserWalletId, token: CryptoCurrency.Token): Either<GetFeeError, BigDecimal> =
        either {
            val source = source(userWalletId, token).bind()
            val value = source.status.value
            val balance = ensureNotNull(value.amount) { GetFeeError.UnknownError }
            val accountAddress = ensureNotNull(value.networkAddress?.defaultAddress?.value) { GetFeeError.UnknownError }
            quote(source = source, amount = balance, destination = accountAddress).bind().feeAmount.roundedUpToCents()
        }

    suspend fun send(
        userWalletId: UserWalletId,
        token: CryptoCurrency.Token,
        amount: BigDecimal,
        destination: String,
        maxNetworkFee: BigDecimal,
    ): Either<MoneyAccountTransferError, Unit> = either {
        val source = source(userWalletId, token).getOrElse { notSent(step = "account lookup", cause = it) }
        val quote = quote(source = source, amount = amount, destination = destination)
            .getOrElse { notSent(step = "network fee", cause = it) }
        if (quote.feeAmount > maxNetworkFee) {
            TangemLogger.withTag(TAG).i("Money account transfer held, network fee ${quote.feeAmount} > $maxNetworkFee")
            raise(MoneyAccountTransferError.NetworkFeeIncreased(quote.feeAmount.roundedUpToCents()))
        }
        val balance = source.status.value.amount ?: notSent(step = "balance lookup", cause = "no amount")
        val total = amount + quote.feeAmount
        if (total > balance) notSent(step = "balance check", cause = "$total > $balance")
        val feeTransaction = createTransferTransactionUseCase(
            amount = amount.convertToSdkAmount(source.status),
            fee = quote.fee.transactionFee.normal,
            memo = null,
            destination = destination,
            userWalletId = userWalletId,
            network = token.network,
        ).getOrElse { notSent(step = "transaction build", cause = it) }
        val txHash = createAndSendGaslessTransactionUseCase(
            userWallet = source.userWallet,
            transactionData = feeTransaction,
            fee = quote.fee,
        ).getOrElse { error -> raise(error.toTransferError()) }
        TangemLogger.withTag(TAG).i("Money account transfer sent: $txHash", shouldSanitize = false)
    }

    private suspend fun source(userWalletId: UserWalletId, token: CryptoCurrency.Token): Either<GetFeeError, Source> =
        either {
            val userWallet = getUserWalletUseCase(userWalletId).mapLeft { GetFeeError.UnknownError }.bind()
            val status = ensureNotNull(
                getMoneyAccountCryptoCurrencyStatusUseCase.invokeSync(userWalletId, token).getOrNull()?.second,
            ) { GetFeeError.UnknownError }
            Source(userWalletId = userWalletId, userWallet = userWallet, token = token, status = status)
        }

    private suspend fun quote(source: Source, amount: BigDecimal, destination: String): Either<GetFeeError, Quote> =
        either {
            val transaction = createTransferTransactionUseCase(
                amount = amount.convertToSdkAmount(source.status),
                memo = null,
                destination = destination,
                userWalletId = source.userWalletId,
                network = source.token.network,
            ).mapLeft(GetFeeError::DataError).bind()
            val fee = getFeeForTokenUseCase(
                userWallet = source.userWallet,
                token = source.token,
                transactionData = transaction,
                sentAmount = amount,
            ).bind()
            val feeAmount = ensureNotNull(fee.transactionFee.normal.amount.value) { GetFeeError.UnknownError }
            Quote(fee = fee, feeAmount = feeAmount)
        }

    private fun Raise<MoneyAccountTransferError>.notSent(step: String, cause: Any): Nothing {
        TangemLogger.withTag(TAG).e("Money account transfer not sent, $step failed: $cause")
        raise(MoneyAccountTransferError.NotSent)
    }

    private fun SendTransactionError.toTransferError(): MoneyAccountTransferError {
        TangemLogger.withTag(TAG).e("Money account transfer failed: $this")
        return when (this) {
            SendTransactionError.UserCancelledError -> MoneyAccountTransferError.Cancelled
            is SendTransactionError.NetworkError -> MoneyAccountTransferError.OutcomeUnknown
            else -> MoneyAccountTransferError.NotSent
        }
    }

    private class Source(
        val userWalletId: UserWalletId,
        val userWallet: UserWallet,
        val token: CryptoCurrency.Token,
        val status: CryptoCurrencyStatus,
    )

    private class Quote(val fee: TransactionFeeExtended, val feeAmount: BigDecimal)

    private fun BigDecimal.roundedUpToCents(): BigDecimal = setScale(CENTS_SCALE, RoundingMode.CEILING)
}

internal sealed interface MoneyAccountTransferError {

    data object Cancelled : MoneyAccountTransferError

    data object NotSent : MoneyAccountTransferError

    data object OutcomeUnknown : MoneyAccountTransferError

    data class NetworkFeeIncreased(val networkFee: BigDecimal) : MoneyAccountTransferError
}

private const val TAG = "MoneyAccountTransfer"
private const val CENTS_SCALE = 2