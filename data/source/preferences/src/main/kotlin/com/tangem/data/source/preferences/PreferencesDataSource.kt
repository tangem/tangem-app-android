package com.tangem.data.source.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.data.source.preferences.adapters.BigDecimalAdapter
import com.tangem.data.source.preferences.adapters.CardBalanceStateAdapter
import com.tangem.data.source.preferences.storage.DisclaimerPrefStorage
import com.tangem.data.source.preferences.storage.FiatCurrenciesPrefStorage
import com.tangem.data.source.preferences.storage.ToppedUpWalletStorage
import com.tangem.data.source.preferences.storage.UsedCardsPrefStorage
import javax.inject.Inject

// 🔥FIXME: Only logic to work with preferences must be here, must be separated to repositories
// TODO: Replace shared preferences with DataStore
@Deprecated("Create repository instead")
class PreferencesDataSource @Inject internal constructor(applicationContext: Context) {

    val appRatingLaunchObserver: AppRatingLaunchObserver
    val usedCardsPrefStorage: UsedCardsPrefStorage
    val fiatCurrenciesPrefStorage: FiatCurrenciesPrefStorage
    val disclaimerPrefStorage: DisclaimerPrefStorage
    val toppedUpWalletStorage: ToppedUpWalletStorage

    private val preferences: SharedPreferences =
        applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val moshiConverter = MoshiJsonConverter(
        adapters = listOf(BigDecimalAdapter(), CardBalanceStateAdapter()) + MoshiJsonConverter.getTangemSdkAdapters(),
        typedAdapters = MoshiJsonConverter.getTangemSdkTypedAdapters(),
    )

    init {
        incrementLaunchCounter()
        appRatingLaunchObserver = AppRatingLaunchObserver(preferences, getCountOfLaunches())
        usedCardsPrefStorage = UsedCardsPrefStorage(preferences, moshiConverter)
        usedCardsPrefStorage.migrate()
        fiatCurrenciesPrefStorage = FiatCurrenciesPrefStorage(preferences, moshiConverter)
        fiatCurrenciesPrefStorage.migrate()
        disclaimerPrefStorage = DisclaimerPrefStorage(preferences)
        toppedUpWalletStorage = ToppedUpWalletStorage(preferences, moshiConverter)
    }

    var zendeskFirstLaunchTime: Long?
        get() = preferences.getLong(ZENDESK_FIRST_LAUNCH_KEY, 0).takeIf { it != 0L }
        set(value) = preferences.edit { putLong(ZENDESK_FIRST_LAUNCH_KEY, value ?: 0) }

    var sprinklrFirstLaunchTime: Long?
        get() = preferences.getLong(SPRINKLR_FIRST_LAUNCH_KEY, 0).takeIf { it != 0L }
        set(value) = preferences.edit { putLong(SPRINKLR_FIRST_LAUNCH_KEY, value ?: 0) }

    var shouldShowSaveUserWalletScreen: Boolean
        get() = preferences.getBoolean(SAVE_WALLET_DIALOG_SHOWN_KEY, true)
        set(value) = preferences.edit {
            putBoolean(SAVE_WALLET_DIALOG_SHOWN_KEY, value)
        }

    var shouldSaveUserWallets: Boolean
        get() = preferences.getBoolean(SAVE_USER_WALLETS_KEY, false)
        set(value) = preferences.edit {
            putBoolean(SAVE_USER_WALLETS_KEY, value)
        }

    var shouldSaveAccessCodes: Boolean
        get() = preferences.getBoolean(SAVE_ACCESS_CODES_KEY, false)
        set(value) = preferences.edit {
            putBoolean(SAVE_ACCESS_CODES_KEY, value)
        }

    var wasApplicationStopped: Boolean
        get() = preferences.getBoolean(APPLICATION_STOPPED_KEY, false)
        set(value) = preferences.edit {
            putBoolean(APPLICATION_STOPPED_KEY, value)
        }

    var shouldOpenWelcomeScreenOnResume: Boolean
        get() = preferences.getBoolean(OPEN_WELCOME_ON_RESUME_KEY, false)
        set(value) = preferences.edit {
            putBoolean(OPEN_WELCOME_ON_RESUME_KEY, value)
        }

    fun saveTwinsOnboardingShown() {
        preferences.edit { putBoolean(TWINS_ONBOARDING_SHOWN_KEY, true) }
    }

    fun wasTwinsOnboardingShown(): Boolean {
        return preferences.getBoolean(TWINS_ONBOARDING_SHOWN_KEY, false)
    }

    private fun getCountOfLaunches(): Int = preferences.getInt(APP_LAUNCH_COUNT_KEY, 1)

    private fun incrementLaunchCounter() {
        var count = preferences.getInt(APP_LAUNCH_COUNT_KEY, 0)
        preferences.edit { putInt(APP_LAUNCH_COUNT_KEY, ++count) }
    }

    companion object {
        private const val PREFERENCES_NAME = "tapPrefs"
        private const val TWINS_ONBOARDING_SHOWN_KEY = "twinsOnboardingShown"
        private const val APP_LAUNCH_COUNT_KEY = "launchCount"
        private const val ZENDESK_FIRST_LAUNCH_KEY = "chatFirstLaunchKey"
        private const val SPRINKLR_FIRST_LAUNCH_KEY = "sprinklrFirstLaunch"
        private const val SAVE_WALLET_DIALOG_SHOWN_KEY = "saveUserWalletShown"
        private const val SAVE_USER_WALLETS_KEY = "saveUserWallets"
        private const val SAVE_ACCESS_CODES_KEY = "saveAccessCodes"
        private const val APPLICATION_STOPPED_KEY = "applicationStopped"
        private const val OPEN_WELCOME_ON_RESUME_KEY = "openWelcomeOnResume"
    }
}