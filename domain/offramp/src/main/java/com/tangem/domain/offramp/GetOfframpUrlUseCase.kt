package com.tangem.domain.offramp

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.offramp.repository.OfframpRepository
import com.tangem.utils.logging.TangemLogger

/**
 * Use case for getting offramp (sell crypto) URL.
 *
 * Registers a single-use `request_id` in [OfframpRepository] and embeds it into the provider redirect URL so the
 * returning `redirect_sell` deeplink can be validated as a real, user-initiated sell.
 *
 * @property offrampRepository repository for offramp operations
 */
class GetOfframpUrlUseCase(
    private val offrampRepository: OfframpRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        appCurrencyCode: String,
    ): Either<Error, String> = either<Error, String> {
        val walletAddress = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value
        ensure(walletAddress != null) { Error.WalletAddressNotFound }

        val requestId = offrampRepository.registerPendingOfframp(
            userWalletId = userWalletId,
            currencyId = cryptoCurrencyStatus.currency.id.value,
        )

        val url = offrampRepository.getOfframpUrl(
            cryptoCurrency = cryptoCurrencyStatus.currency,
            fiatCurrencyCode = appCurrencyCode,
            walletAddress = walletAddress,
            requestId = requestId,
        )
        ensure(url != null) { Error.UrlNotAvailable }

        url
    }
        .onLeft { TangemLogger.e("Error getting offramp URL: $it") }

    /** Offramp use case errors */
    sealed class Error {
        /** Wallet address not found in currency status */
        data object WalletAddressNotFound : Error()

        /** Offramp URL is not available for this currency */
        data object UrlNotAvailable : Error()
    }
}