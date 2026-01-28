package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.left
import com.tangem.core.error.UniversalError
import com.tangem.data.common.quote.QuotesFetcher
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.request.WithdrawDataRequest
import com.tangem.datasource.api.pay.models.request.WithdrawRequest
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.WithdrawalResult
import com.tangem.domain.pay.WithdrawalSignatureResult
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.pay.repository.TangemPaySwapRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.extensions.addHexPrefix
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

@Suppress("LongParameterList")
internal class DefaultTangemPaySwapRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
    private val authDataSource: TangemPayAuthDataSource,
    private val quotesFetcher: QuotesFetcher,
    private val tangemPayStorage: TangemPayStorage,
) : TangemPaySwapRepository {

    override suspend fun withdraw(
        userWallet: UserWallet,
        receiverAddress: String,
        cryptoAmount: BigDecimal,
        cryptoCurrencyId: CryptoCurrency.RawID,
    ): Either<UniversalError, WithdrawalResult> {
        val amountInCents = getAmountInCents(cryptoAmount, cryptoCurrencyId)
        if (amountInCents.isNullOrEmpty()) return Either.Left(VisaApiError.WithdrawalDataError)
        return requestHelper.performRequest(userWallet.walletId) { authHeader ->
            val request = WithdrawDataRequest(amountInCents = amountInCents, recipientAddress = receiverAddress)
            tangemPayApi.getWithdrawData(authHeader = authHeader, body = request)
        }.map { data ->
            val result = data.result ?: return VisaApiError.WithdrawalDataError.left()
            val signatureResult = authDataSource.getWithdrawalSignature(
                userWallet = userWallet,
                hash = result.hash,
            ).getOrNull()

            return when (signatureResult) {
                is WithdrawalSignatureResult.Cancelled -> {
                    Either.Right(WithdrawalResult.Cancelled)
                }
                is WithdrawalSignatureResult.Success -> {
                    requestHelper.performRequest(userWallet.walletId) { authHeader ->
                        val request = WithdrawRequest(
                            amountInCents = amountInCents,
                            recipientAddress = receiverAddress,
                            adminSalt = result.salt,
                            senderAddress = result.senderAddress,
                            adminSignature = signatureResult.signature.addHexPrefix(),
                        )
                        tangemPayApi.withdraw(authHeader = authHeader, body = request)
                    }
                        .mapLeft { return Either.Left(VisaApiError.WithdrawError) }
                        .map { response ->
                            val orderId = response.result?.orderId
                            if (orderId != null) tangemPayStorage.storeWithdrawOrder(userWallet.walletId, orderId)
                            WithdrawalResult.Success
                        }
                }
                null -> return Either.Left(VisaApiError.SignWithdrawError)
            }
        }
    }

    private suspend fun getAmountInCents(cryptoAmount: BigDecimal, cryptoCurrencyId: CryptoCurrency.RawID): String? {
        val fiatRate = getFiatRate(cryptoCurrencyId) ?: return null
        val amountInDollars = cryptoAmount.multiply(fiatRate)
        val defaultFractionDigits = Currency.getInstance(Locale.US).defaultFractionDigits
        return amountInDollars
            .setScale(defaultFractionDigits, RoundingMode.HALF_UP)
            .movePointRight(defaultFractionDigits)
            .longValueExact()
            .toString()
    }

    private suspend fun getFiatRate(cryptoCurrencyId: CryptoCurrency.RawID): BigDecimal? {
        val quotes = quotesFetcher.fetch(
            fiatCurrencyId = Currency.getInstance(Locale.US).currencyCode,
            currencyId = cryptoCurrencyId.value,
            field = QuotesFetcher.Field.PRICE,
        ).getOrNull()

        return quotes?.quotes[cryptoCurrencyId.value]?.price
    }
}