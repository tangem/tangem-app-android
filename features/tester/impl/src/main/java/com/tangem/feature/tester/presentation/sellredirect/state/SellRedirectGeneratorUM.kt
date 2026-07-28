package com.tangem.feature.tester.presentation.sellredirect.state

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Content state of the Sell Redirect DeepLink generator screen.
 *
 * The screen reads every locally-stored app-initiated sell (pending off-ramps, including already-expired ones) and,
 * for each one, builds a `redirect_sell` deeplink carrying its real `request_id` — the only value that lets the
 * deeplink pass the app's authenticity check. The remaining parameters (transaction id, amount, deposit address) are
 * filled with test placeholders since they are not part of the stored record.
 *
 * @property onBackClick    invoked when the back button is pressed
 * @property onRefreshClick invoked to reload the stored sells
 * @property items          one generated deeplink per stored sell, newest first
 * @property isEmpty        `true` once loading finished and no stored sell was found
 */
internal data class SellRedirectGeneratorUM(
    val onBackClick: () -> Unit = {},
    val onRefreshClick: () -> Unit = {},
    val items: ImmutableList<DeepLinkItemUM> = persistentListOf(),
    val isEmpty: Boolean = false,
) {

    /**
     * A single generated deeplink built from one cached sell.
     *
     * @property currencyId  currency id the sell was registered for
     * @property walletId    shortened id of the wallet that registered the sell (must be the selected wallet for the
     *                       deeplink to be accepted)
     * @property requestId   shortened nonce embedded in the deeplink
     * @property age         human-readable age of the record (e.g. `5m ago`)
     * @property deepLink    full generated `tangem://redirect_sell?...` URL
     * @property isExpired   `true` when the record is past its expiry — the deeplink will no longer be accepted
     * @property onCopyClick copies [deepLink] to the clipboard
     * @property onOpenClick fires [deepLink] as a VIEW intent so it routes through the app's deeplink handling
     */
    data class DeepLinkItemUM(
        val currencyId: String,
        val walletId: String,
        val requestId: String,
        val age: String,
        val deepLink: String,
        val isExpired: Boolean,
        val onCopyClick: () -> Unit,
        val onOpenClick: () -> Unit,
    )
}