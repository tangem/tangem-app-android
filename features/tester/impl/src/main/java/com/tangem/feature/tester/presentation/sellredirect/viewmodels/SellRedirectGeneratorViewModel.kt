package com.tangem.feature.tester.presentation.sellredirect.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.common.routing.DeepLinkRoute
import com.tangem.common.routing.DeepLinkScheme
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.offramp.model.PendingOfframp
import com.tangem.domain.offramp.repository.OfframpRepository
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.feature.tester.presentation.sellredirect.state.SellRedirectGeneratorUM
import com.tangem.feature.tester.presentation.sellredirect.state.SellRedirectGeneratorUM.DeepLinkItemUM
import com.tangem.utils.coroutines.runSuspendCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * ViewModel for the Sell Redirect DeepLink generator screen.
 *
 * Reads every stored app-initiated sell (pending off-ramps, including expired ones) via [OfframpRepository] and turns
 * each into a ready-to-use `redirect_sell` deeplink. The stored `request_id` is the crucial part: only a deeplink
 * carrying a real, app-issued `request_id` (bound to the same wallet + currency) survives the handler's authenticity
 * check. Expired records are still listed but flagged, since they will no longer be accepted.
 */
@HiltViewModel
internal class SellRedirectGeneratorViewModel @Inject constructor(
    private val offrampRepository: OfframpRepository,
    private val clipboardManager: ClipboardManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val uiState: StateFlow<SellRedirectGeneratorUM>
        field = MutableStateFlow(SellRedirectGeneratorUM(onRefreshClick = ::load))

    init {
        load()
    }

    /** Setup navigation state property by router [router] */
    fun setupNavigation(router: InnerTesterRouter) {
        uiState.update { it.copy(onBackClick = router::back) }
    }

    private fun load() {
        viewModelScope.launch {
            val stored = runSuspendCatching { offrampRepository.getAllStoredOfframps() }
                .onFailure {
                    Toast.makeText(context, "Failed to read stored sells: ${it.message}", Toast.LENGTH_SHORT).show()
                }
                .getOrDefault(emptyList())
            val now = System.currentTimeMillis()
            val items = stored
                .sortedByDescending { it.createdAt }
                .map { it.toItem(now) }
                .toImmutableList()
            uiState.update { it.copy(items = items, isEmpty = items.isEmpty()) }
        }
    }

    private fun PendingOfframp.toItem(now: Long): DeepLinkItemUM {
        val deepLink = buildDeepLink(this)
        return DeepLinkItemUM(
            currencyId = currencyId,
            walletId = userWalletId.stringValue.shorten(),
            requestId = requestId.shorten(),
            age = formatAge(createdAt, now),
            deepLink = deepLink,
            isExpired = isExpired(now),
            onCopyClick = { copyDeepLink(deepLink) },
            onOpenClick = { openDeepLink(deepLink) },
        )
    }

    /**
     * Builds a `tangem://redirect_sell?...` URL. The real cached [PendingOfframp.currencyId] / [requestId] gate the
     * handler's authenticity check; the other required params are non-empty test placeholders (the transaction id,
     * amount and deposit address are not part of the cached record).
     */
    private fun buildDeepLink(offramp: PendingOfframp): String = Uri.Builder()
        .scheme(DeepLinkScheme.Tangem.scheme)
        .authority(DeepLinkRoute.SellRedirect.host)
        .appendQueryParameter(CURRENCY_ID_KEY, offramp.currencyId)
        .appendQueryParameter(REQUEST_ID_KEY, offramp.requestId)
        .appendQueryParameter(TRANSACTION_ID_KEY, "test-tx-${offramp.requestId.take(TX_ID_LENGTH)}")
        .appendQueryParameter(AMOUNT_KEY, DEFAULT_AMOUNT)
        .appendQueryParameter(DESTINATION_ADDRESS_KEY, DEFAULT_ADDRESS)
        .build()
        .toString()

    private fun copyDeepLink(deepLink: String) {
        clipboardManager.setText(text = deepLink, isSensitive = false, label = "Sell redirect deeplink")
        Toast.makeText(context, "Deeplink copied", Toast.LENGTH_SHORT).show()
    }

    private fun openDeepLink(deepLink: String) {
        val intent = Intent(Intent.ACTION_VIEW, deepLink.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { Toast.makeText(context, "Can't open: ${it.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun formatAge(createdAt: Long, now: Long): String {
        val elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(now - createdAt)
        return when {
            elapsedMinutes <= 0 -> "just now"
            else -> "${elapsedMinutes}m ago"
        }
    }

    /** Shortens a long value to `prefix…suffix` for display; the full value stays inside the deeplink. */
    private fun String.shorten(): String =
        if (length <= SHORTEN_KEEP * 2 + 1) this else "${take(SHORTEN_KEEP)}…${takeLast(SHORTEN_KEEP)}"

    private companion object {
        const val SHORTEN_KEEP = 6
        const val TX_ID_LENGTH = 8
        const val DEFAULT_AMOUNT = "1"
        const val DEFAULT_ADDRESS = "test-deposit-address"

        // Query keys expected by DefaultSellRedirectDeepLinkHandler; kept in sync with it.
        const val TRANSACTION_ID_KEY = "transactionId"
        const val CURRENCY_ID_KEY = "currency_id"
        const val AMOUNT_KEY = "baseCurrencyAmount"
        const val DESTINATION_ADDRESS_KEY = "depositWalletAddress"
        const val REQUEST_ID_KEY = "request_id"
    }
}