package com.tangem.datasource.local.preferences

import androidx.datastore.preferences.core.*
import com.tangem.datasource.local.preferences.PreferencesKeys.APP_LAUNCH_COUNT_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.FUNDS_FOUND_DATE_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.SAVE_USER_WALLETS_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.SHOW_RATING_DIALOG_AT_LAUNCH_COUNT_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.USED_CARDS_INFO_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.USER_WAS_INTERACT_WITH_RATING_KEY

/**
 * All preferences keys that DataStore<Preferences> is stored.
 *
[REDACTED_AUTHOR]
 */
object PreferencesKeys {

    val SAVE_USER_WALLETS_KEY by lazy { booleanPreferencesKey(name = "saveUserWallets") }

    val APP_LAUNCH_COUNT_KEY by lazy { intPreferencesKey(name = "launchCount") }

    val SHOW_RATING_DIALOG_AT_LAUNCH_COUNT_KEY by lazy { intPreferencesKey(name = "showRatingDialogAtLaunchCount") }

    val FUNDS_FOUND_DATE_KEY by lazy { longPreferencesKey(name = "fundsFoundDate") }

    val USER_WAS_INTERACT_WITH_RATING_KEY by lazy { booleanPreferencesKey(name = "userWasInteractWithRating") }

    val USED_CARDS_INFO_KEY by lazy { stringPreferencesKey(name = "usedCardsInfo_v2") }

    val APP_THEME_MODE_KEY by lazy { stringPreferencesKey(name = "appThemeMode") }

    val SELECTED_APP_CURRENCY_KEY by lazy { stringPreferencesKey(name = "selectedAppCurrency") }

    val BALANCE_HIDING_SETTINGS_KEY by lazy { stringPreferencesKey(name = "balanceHidingSettings") }

    val SWAP_TRANSACTIONS_KEY by lazy { stringPreferencesKey(name = "swapTransactions") }

    val WALLETS_SCROLL_PREVIEW_KEY by lazy { booleanPreferencesKey(name = "walletsScrollPreview") }

    val SENT_ONE_TIME_EVENTS_KEY by lazy { stringPreferencesKey(name = "sentOneTimeEvents") }

    val WALLETS_BALANCES_STATES_KEY by lazy { stringPreferencesKey(name = "walletsBalancesStates") }

    val LAST_SWAPPED_CRYPTOCURRENCY_ID_KEY by lazy { stringPreferencesKey(name = "lastSwappedCryptoCurrency") }

    val IS_WALLET_SWAP_PROMO_SHOW_KEY by lazy { booleanPreferencesKey(name = "isWalletSwapPromoShown") }

    val IS_TOKEN_SWAP_PROMO_SHOW_KEY by lazy { stringSetPreferencesKey(name = "isTokenSwapPromoShown") }
}

/** Preferences keys set that should be migrated from "PreferencesDataSource" to a new DataStore<Preferences> */
internal fun getTapPrefKeysToMigrate(): Set<String> {
    return setOf(
        SAVE_USER_WALLETS_KEY,
        APP_LAUNCH_COUNT_KEY,
        SHOW_RATING_DIALOG_AT_LAUNCH_COUNT_KEY,
        FUNDS_FOUND_DATE_KEY,
        USER_WAS_INTERACT_WITH_RATING_KEY,
        USED_CARDS_INFO_KEY,
    )
        .map(Preferences.Key<*>::name)
        .toSet()
}