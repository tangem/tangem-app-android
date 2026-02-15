package com.tangem.domain.offramp

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.offramp.repository.OfframpRepository

/**
 * Use case for getting offramp (sell crypto) URL
 *
 * @property offrampRepository repository for offramp operations
 */
class GetOfframpUrlUseCase(
    private val offrampRepository: OfframpRepository,
) {

    operator fun invoke(cryptoCurrencyStatus: CryptoCurrencyStatus, appCurrencyCode: String): Either<Error, String> =
        either {
            val walletAddress = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value
            ensure(walletAddress != null) { Error.WalletAddressNotFound }

            val url = offrampRepository.getOfframpUrl(
                cryptoCurrency = cryptoCurrencyStatus.currency,
                fiatCurrencyCode = appCurrencyCode,
                walletAddress = walletAddress,
            )
            ensure(url != null) { Error.UrlNotAvailable }

            url
        }

    /** Offramp use case errors */
    sealed class Error {
        /** Wallet address not found in currency status */
        data object WalletAddressNotFound : Error()

        /** Offramp URL is not available for this currency */
        data object UrlNotAvailable : Error()
    }
}