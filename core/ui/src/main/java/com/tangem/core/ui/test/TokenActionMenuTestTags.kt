package com.tangem.core.ui.test

/**
 * Test tags for the token context menu (long-tap on a token row) actions.
 *
 * Each action row keeps the generic [BaseBottomSheetTestTags.ACTION_BUTTON] tag (for counting), while its
 * label additionally carries a per-action tag built from the action id via [action]. The id equals the
 * `TokenActionsState.ActionState` subclass simple name (see `MultiWalletCurrencyActionsConverter`), so the
 * constants below must stay in sync with those class names.
 */
object TokenActionMenuTestTags {

    private const val PREFIX = "TOKEN_ACTION_MENU_"

    const val ANALYTICS = "Analytics"
    const val COPY_ADDRESS = "CopyAddress"
    const val RECEIVE = "Receive"
    const val SEND = "Send"
    const val SWAP = "Swap"
    const val BUY = "Buy"
    const val SELL = "Sell"
    const val HIDE_TOKEN = "HideToken"

    /** Per-action tag for an action with the given [id] (an ActionState subclass simple name). */
    fun action(id: String): String = "$PREFIX$id"
}