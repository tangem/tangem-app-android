package com.tangem.tap.data

import androidx.datastore.core.DataStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.offramp.model.PendingOfframp
import com.tangem.domain.offramp.model.PendingOfframp.Companion.EXPIRY_MS
import com.tangem.domain.offramp.repository.OfframpRepository
import com.tangem.tap.common.apptheme.MutableAppThemeModeHolder
import com.tangem.tap.data.converter.PendingOfframpEntryConverter
import com.tangem.tap.data.model.PendingOfframpEntry
import com.tangem.tap.network.exchangeServices.SellService
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Default implementation of [OfframpRepository].
 *
 * @property sellService sell service for getting offramp URL
 * @property pendingOfframpStore dedicated kotlinx-serialized store of app-initiated sells
 * @property dispatchers coroutine dispatchers provider for IO operations
 */
internal class DefaultOfframpRepository(
    private val sellService: SellService,
    private val pendingOfframpStore: DataStore<List<PendingOfframpEntry>>,
    private val dispatchers: CoroutineDispatcherProvider,
) : OfframpRepository {

    private val converter = PendingOfframpEntryConverter()

    override fun getOfframpUrl(
        cryptoCurrency: CryptoCurrency,
        fiatCurrencyCode: String,
        walletAddress: String,
        requestId: String,
    ): String? {
        return sellService.getUrl(
            cryptoCurrency = cryptoCurrency,
            fiatCurrencyName = fiatCurrencyCode,
            walletAddress = walletAddress,
            isDarkTheme = MutableAppThemeModeHolder.isDarkThemeActive,
            requestId = requestId,
        )
    }

    override suspend fun registerPendingOfframp(userWalletId: UserWalletId, currencyId: String): String =
        withContext(dispatchers.io) {
            val requestId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            pendingOfframpStore.updateData { stored ->
                stored.filterNotExpired(now) + PendingOfframpEntry(
                    requestId = requestId,
                    userWalletId = userWalletId.stringValue,
                    currencyId = currencyId,
                    createdAt = now,
                )
            }
            requestId
        }

    override suspend fun resolvePendingOfframp(
        requestId: String,
        userWalletId: UserWalletId,
        currencyId: String,
    ): PendingOfframp? = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        var matched: PendingOfframpEntry? = null
        pendingOfframpStore.updateData { stored ->
            matched = stored.firstOrNull { entry ->
                entry.requestId == requestId &&
                    entry.userWalletId == userWalletId.stringValue &&
                    entry.currencyId == currencyId &&
                    !entry.isExpired(now)
            }
            // Keep the matched record so the same redirect can be followed again until it expires; only prune the
            // expired ones. The record is dropped naturally once it ages past EXPIRY_MS.
            stored.filterNotExpired(now)
        }
        matched?.let(converter::convert)
    }

    override suspend fun getAllStoredOfframps(): List<PendingOfframp> = withContext(dispatchers.io) {
        pendingOfframpStore.data.first().map(converter::convert)
    }

    // Returns the same instance when nothing is expired, so DataStore.updateData sees an unchanged value and skips
    // both the extra allocation and the write.
    private fun List<PendingOfframpEntry>.filterNotExpired(now: Long): List<PendingOfframpEntry> =
        if (none { now - it.createdAt >= EXPIRY_MS }) this else filter { now - it.createdAt < EXPIRY_MS }

    private fun PendingOfframpEntry.isExpired(now: Long): Boolean = converter.convert(this).isExpired(now)
}