package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.right
import com.tangem.domain.models.pay.TangemPayCountry
import com.tangem.domain.models.pay.TangemPayRegion
import com.tangem.domain.pay.repository.TangemPayResidencyRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.SupportedLanguages
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**

 * The lists come back localized (the BFF sends Accept-Language), so a cached list belongs to the language it was
 * fetched in.
 */
internal class CachingTangemPayResidencyRepository(
    private val origin: TangemPayResidencyRepository,
    private val currentLanguage: () -> String = SupportedLanguages::getCurrentSupportedLanguageCode,
) : TangemPayResidencyRepository by origin {

    private val mutex = Mutex()
    private val regions = mutableMapOf<String, List<TangemPayRegion>>()
    private var language: String? = null
    private var countries: List<TangemPayCountry>? = null

    override suspend fun getCountries(): Either<VisaApiError, List<TangemPayCountry>> {
        return mutex.withLock {
            dropCacheOnLanguageChange()
            countries?.let { return@withLock it.right() }

            origin.getCountries().onRight { loaded -> countries = loaded }
        }
    }

    override suspend fun getRegions(countryCode: String): Either<VisaApiError, List<TangemPayRegion>> {
        return mutex.withLock {
            dropCacheOnLanguageChange()
            val key = countryCode.uppercase()
            regions[key]?.let { return@withLock it.right() }

            origin.getRegions(countryCode).onRight { loaded -> regions[key] = loaded }
        }
    }

    private fun dropCacheOnLanguageChange() {
        val current = currentLanguage()
        if (language == current) return

        language = current
        countries = null
        regions.clear()
    }
}