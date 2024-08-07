package com.tangem.datasource.local.preferences

import androidx.datastore.preferences.core.*
import com.tangem.datasource.local.preferences.PreferencesKeys.APP_LAUNCH_COUNT_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.FUNDS_FOUND_DATE_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.IS_TANGEM_TOS_ACCEPTED_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.SAVE_USER_WALLETS_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.SHOULD_OPEN_WELCOME_ON_RESUME_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.SHOULD_SAVE_ACCESS_CODES_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.SHOULD_SHOW_SAVE_USER_WALLET_SCREEN_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.SHOW_RATING_DIALOG_AT_LAUNCH_COUNT_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.USED_CARDS_INFO_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.USER_WAS_INTERACT_WITH_RATING_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.WAS_APPLICATION_STOPPED_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.WAS_TWINS_ONBOARDING_SHOWN

/**
 * All preferences keys that DataStore<Preferences> is stored.
 *
[REDACTED_AUTHOR]
 */
object PreferencesKeys {

    val SAVE_USER_WALLETS_KEY by lazy { booleanPreferencesKey(name = "saveUserWallets") }

    val SHOULD_SHOW_SAVE_USER_WALLET_SCREEN_KEY by lazy { booleanPreferencesKey("saveUserWalletShown") }

    val APP_LAUNCH_COUNT_KEY by lazy { intPreferencesKey(name = "launchCount") }

    val SHOW_RATING_DIALOG_AT_LAUNCH_COUNT_KEY by lazy { intPreferencesKey(name = "showRatingDialogAtLaunchCount") }

    val FUNDS_FOUND_DATE_KEY by lazy { longPreferencesKey(name = "fundsFoundDate") }

    val USER_WAS_INTERACT_WITH_RATING_KEY by lazy { booleanPreferencesKey(name = "userWasInteractWithRating") }

    val USED_CARDS_INFO_KEY by lazy { stringPreferencesKey(name = "usedCardsInfo_v2") }

    val APP_THEME_MODE_KEY by lazy { stringPreferencesKey(name = "appThemeMode") }

    val SELECTED_APP_CURRENCY_KEY by lazy { stringPreferencesKey(name = "selectedAppCurrency") }

    val BALANCE_HIDING_SETTINGS_KEY by lazy { stringPreferencesKey(name = "balanceHidingSettings") }

    val SWAP_TRANSACTIONS_KEY by lazy { stringPreferencesKey(name = "swapTransactions") }

    val SWAP_TRANSACTIONS_STATUSES_KEY by lazy { stringPreferencesKey(name = "swapTransactionsStatuses") }

    val WALLETS_SCROLL_PREVIEW_KEY by lazy { booleanPreferencesKey(name = "walletsScrollPreview") }

    val SENT_ONE_TIME_EVENTS_KEY by lazy { stringPreferencesKey(name = "sentOneTimeEvents") }

    val WALLETS_BALANCES_STATES_KEY by lazy { stringPreferencesKey(name = "walletsBalancesStates") }

    val LAST_SWAPPED_CRYPTOCURRENCY_ID_KEY by lazy { stringPreferencesKey(name = "lastSwappedCryptoCurrency") }

    val IS_WALLET_TRAVALA_PROMO_SHOWN_KEY by lazy {
        booleanPreferencesKey(name = "isWalletTravalaPromoShown")
    }

    val FEATURE_TOGGLES_KEY by lazy { stringPreferencesKey(name = "featureToggles") }

    val WAS_TWINS_ONBOARDING_SHOWN by lazy { booleanPreferencesKey(name = "twinsOnboardingShown") }

    val IS_TANGEM_TOS_ACCEPTED_KEY by lazy { booleanPreferencesKey(name = "tangem_tos_accepted") }

    val APP_LOGS_KEY by lazy { stringPreferencesKey(name = "app_logs") }

    val POLKADOT_HEALTH_CHECK_LAST_INDEXED_TX_KEY by lazy {
        stringPreferencesKey(name = "POLKADOT_HEALTH_CHECK_LAST_INDEXED_TX")
    }
    val POLKADOT_HEALTH_CHECKED_RESET_ACCOUNTS_KEY by lazy {
        stringPreferencesKey(name = "POLKADOT_HEALTH_CHECKED_RESET_ACCOUNTS")
    }
    val POLKADOT_HEALTH_CHECKED_IMMUTABLE_ACCOUNTS_KEY by lazy {
        stringPreferencesKey(name = "POLKADOT_HEALTH_CHECKED_IMMUTABLE_ACCOUNTS")
    }

    val SEND_TAP_HELP_PREVIEW_KEY by lazy { booleanPreferencesKey(name = "sendTapHelpPreview") }

    val WAS_APPLICATION_STOPPED_KEY by lazy { booleanPreferencesKey(name = "applicationStopped") }

    val SHOULD_OPEN_WELCOME_ON_RESUME_KEY by lazy { booleanPreferencesKey(name = "openWelcomeOnResume") }

    val SHOULD_SAVE_ACCESS_CODES_KEY by lazy { booleanPreferencesKey(name = "saveAccessCodes") }

    val IS_WALLET_NAMES_MIGRATION_DONE_KEY by lazy { booleanPreferencesKey(name = "isWalletNamesMigrationDone") }

    val UNSUBMITTED_TRANSACTIONS_KEY by lazy { stringPreferencesKey(name = "unsubmittedTransactions") }

    val IS_WALLET_SWAP_PROMO_OKX_SHOW_KEY by lazy {
        booleanPreferencesKey(name = "isWalletSwapPromoOkxShown")
    }

    val IS_TOKEN_SWAP_PROMO_OKX_SHOW_KEY by lazy {
        booleanPreferencesKey(name = "isTokenSwapPromoOkxShown")
    }

    // region Permission
    fun getShouldShowPermission(permission: String) = booleanPreferencesKey("shouldShowPushPermission_$permission")

    fun getShouldShowInitialPermissionScreen(permission: String) =
        booleanPreferencesKey("shouldShowInitialPushPermissionScreen_$permission")

    fun getIsFirstTimeAskingPermission(permission: String) =
        booleanPreferencesKey("shouldAskInitialPushPermission_$permission")

    fun getPermissionLaunchCount(permission: String) = intPreferencesKey("pushPermissionLaunchCount_$permission")

    fun getPermissionDaysCount(permission: String) = longPreferencesKey("pushPermissionDaysCount_$permission")
    // endregion

    fun getUserTokensKey(userWalletId: String) = stringPreferencesKey(name = "user_tokens_$userWalletId")
}

/** Preferences keys set that should be migrated from "PreferencesDataSource" to a new DataStore<Preferences> */
internal fun getTapPrefKeysToMigrate(): Set<String> {
    return setOf(
        SAVE_USER_WALLETS_KEY,
        SHOULD_SHOW_SAVE_USER_WALLET_SCREEN_KEY,
        APP_LAUNCH_COUNT_KEY,
        SHOW_RATING_DIALOG_AT_LAUNCH_COUNT_KEY,
        FUNDS_FOUND_DATE_KEY,
        USER_WAS_INTERACT_WITH_RATING_KEY,
        USED_CARDS_INFO_KEY,
        WAS_TWINS_ONBOARDING_SHOWN,
        IS_TANGEM_TOS_ACCEPTED_KEY,
        WAS_APPLICATION_STOPPED_KEY,
        SHOULD_OPEN_WELCOME_ON_RESUME_KEY,
        SHOULD_SAVE_ACCESS_CODES_KEY,
    )
        .map(Preferences.Key<*>::name)
        .toSet()
}