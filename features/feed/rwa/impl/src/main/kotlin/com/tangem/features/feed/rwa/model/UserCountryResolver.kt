package com.tangem.features.feed.rwa.model

import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import java.util.Locale
import javax.inject.Inject

/**
 * Resolves the country the category restrictions are evaluated for.
 *
 * Waits for the start-up country with no timer of its own: it is published once per process and the tab
 * never re-reads it, so a stand-in taken early would name the wrong region for the whole session.
 * `fetchUserCountryCode()` publishes on every path, so the wait ends with the geo request.
 *
 * Returns `null` when the published country is blank.
 */
internal class UserCountryResolver @Inject constructor(
    private val getUserCountryUseCase: GetUserCountryUseCase,
) {

    suspend fun resolve(): String? {
        val resolved = getUserCountryUseCase().mapNotNull { it.getOrNull() }.first()

        // the backend expects upper-case ISO 3166-1 alpha-2, while the published country is lower case
        return resolved.code.uppercase(Locale.ROOT).takeIf(String::isNotBlank)
    }
}