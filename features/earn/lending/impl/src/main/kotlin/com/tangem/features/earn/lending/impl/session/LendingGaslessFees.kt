package com.tangem.features.earn.lending.impl.session

import arrow.core.Either
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCryptoCurrencyStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.models.isGasless
import com.tangem.domain.transaction.usecase.gasless.GetFeeForGaslessUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForTokenUseCase
import java.math.BigDecimal
import javax.inject.Inject

/** Quotes a withdrawal for Smart Gas. Whenever the coin pays, the fee is the session's own levels */
internal class LendingGaslessFees @Inject constructor(
    private val getFeeForGaslessUseCase: GetFeeForGaslessUseCase,
    private val getFeeForTokenUseCase: GetFeeForTokenUseCase,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
) {

    /** @param feeToken the token picked in the selector; without one the coin or a token is chosen automatically */
    suspend fun quote(
        userWallet: UserWallet,
        currency: CryptoCurrency,
        withdrawal: TransactionData.Uncompiled,
        nativeFee: TransactionFee,
        feeToken: CryptoCurrencyStatus?,
    ): Either<GetFeeError, TransactionFeeExtended> {
        val pickedToken = feeToken?.currency as? CryptoCurrency.Token
        val quote = if (pickedToken != null) {
            getFeeForTokenUseCase(userWallet = userWallet, token = pickedToken, transactionData = withdrawal)
        } else {
            getFeeForGaslessUseCase(userWallet = userWallet, network = currency.network, transactionData = withdrawal)
        }
        return quote.map { fee -> if (fee.isGasless) fee.copy(nativeFee = nativeFee) else nativeFee.paidIn(currency) }
    }

    suspend fun balanceOf(userWalletId: UserWalletId, fee: TransactionFeeExtended): BigDecimal? =
        singleAccountStatusListSupplier.getSyncOrNull(userWalletId)
            .getCryptoCurrencyStatus(currencyId = fee.feeTokenId, network = null)
            .getOrNull()
            ?.value
            ?.amount
}

internal fun TransactionFee.paidIn(currency: CryptoCurrency) =
    TransactionFeeExtended(transactionFee = this, feeTokenId = currency.id, nativeFee = this)