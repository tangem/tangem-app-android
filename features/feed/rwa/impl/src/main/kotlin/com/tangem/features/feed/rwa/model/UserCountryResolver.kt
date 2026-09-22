package com.tangem.features.feed.rwa.model

import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.settings.usercountry.models.UserCountry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject

/**
 * Resolves the country the category restrictions are evaluated for.
 *
 * The synchronous read is a `StateFlow` seeded `null` and rebuilt every process, not the persisted value:
 * only the start-up fetch fills it. So **every** launch races the categories request, and the flow wait is
 * the normal path rather than the exception. Waiting briefly beats asking without a country, because such
 * an answer comes back with every category unrestricted — the permissive outcome, and the wrong one to
 * show a user in a restricted region.
 *
 * Returns `null` only when neither the backend nor the device knows the region.
 */
internal class UserCountryResolver @Inject constructor(
    private val getUserCountryUseCase: GetUserCountryUseCase,
) {

    suspend fun resolve(): String? {
        val resolved = getUserCountryUseCase.invokeSync().getOrNull()
            ?: withTimeoutOrNull(RESOLVE_TIMEOUT_MILLIS) {
                getUserCountryUseCase().mapNotNull { it.getOrNull() }.first()
            }
            // Asking without a country makes the backend lift every restriction, so the device region is
            // a better last resort than none — the same order `needApplyFCARestrictions` already uses.
            ?: UserCountry.fromDeviceLocale()

        // ISO 3166-1 alpha-2 is upper case on the wire, while the stored country keeps whatever case it
        // was saved in — normalizing here keeps the cache key stable across both.
        return resolved.code.uppercase(Locale.ROOT).takeIf(String::isNotBlank)
    }

    private companion object {
        const val RESOLVE_TIMEOUT_MILLIS = 3_000L
    }
}