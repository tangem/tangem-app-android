package com.tangem.grow.datasource.preferences

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject

/**
 * DataStore preference keys owned by the Grow stream.
 *

 * and can be garbage-collected, instead of being pinned in memory for the whole process lifetime.
 *
 * The persisted key names must stay identical to the previous core:datasource definitions so existing
 * user data survives the move.
 */
class GrowPreferencesKeys @Inject constructor() {

    val swapTransactionsKey: Preferences.Key<String> = stringPreferencesKey(name = "swapTransactions")

    val swapTransactionsStatusesKey: Preferences.Key<String> =
        stringPreferencesKey(name = "swapTransactionsStatuses")

    val lastSwappedCryptoCurrencyIdKey: Preferences.Key<String> =
        stringPreferencesKey(name = "lastSwappedCryptoCurrency")

    val swapUiModeKey: Preferences.Key<String> = stringPreferencesKey(name = "swapUiMode")

    val unsubmittedTransactionsKey: Preferences.Key<String> = stringPreferencesKey(name = "unsubmittedTransactions")

    val onrampDefaultCountryKey: Preferences.Key<String> = stringPreferencesKey(name = "onrampDefaultCountry")

    val onrampTransactionsStatusesKey: Preferences.Key<String> =
        stringPreferencesKey(name = "onrampTransactionsStatuses")

    val onrampHandledTransactionsKey: Preferences.Key<String> =
        stringPreferencesKey(name = "onrampHandledTransactions")

    val yieldSupplyWarningsStatesKey: Preferences.Key<String> =
        stringPreferencesKey(name = "yieldSupplyWarningsStates")

    val yieldSupplyShouldShowMainPromoKey: Preferences.Key<Boolean> =
        booleanPreferencesKey(name = "yieldSupplyShouldShowMainPromo")

    fun getShouldShowStoriesKey(storyId: String): Preferences.Key<Boolean> =
        booleanPreferencesKey("shouldShowStories_$storyId")
}