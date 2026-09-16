package com.tangem.store.datasource.preferences

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import javax.inject.Inject

/**
 * DataStore preference keys owned by the Store stream.
 *

 * and can be garbage-collected, instead of being pinned in memory for the whole process lifetime.
 *
 * The persisted key names must stay identical to the previous core:datasource definitions so existing
 * user data survives the move.
 */
@Suppress("TooManyFunctions")
class StorePreferencesKeys @Inject constructor() {

    val saveUserWalletsKey: Preferences.Key<Boolean> = booleanPreferencesKey(name = "saveUserWallets")

    val rootDetectedWarningShownKey: Preferences.Key<Boolean> = booleanPreferencesKey(name = "rootDetectedWarningShown")

    val shouldShowAskBiometryKey: Preferences.Key<Boolean> = booleanPreferencesKey("saveUserWalletShown")

    val appLaunchCountKey: Preferences.Key<Int> = intPreferencesKey(name = "launchCount")

    val lastOptionalUpdateShownVersionKey: Preferences.Key<String> =
        stringPreferencesKey(name = "lastOptionalUpdateShownVersion")

    val lastOptionalUpdateShownAtKey: Preferences.Key<Long> = longPreferencesKey(name = "lastOptionalUpdateShownAt")

    val cachedAppVersionsKey: Preferences.Key<String> = stringPreferencesKey(name = "cachedApplicationVersions")

    val cachedAppVersionsAtKey: Preferences.Key<Long> = longPreferencesKey(name = "cachedApplicationVersionsAt")

    val showRatingDialogAtLaunchCountKey: Preferences.Key<Int> =
        intPreferencesKey(name = "showRatingDialogAtLaunchCount")

    val fundsFoundDateKey: Preferences.Key<Long> = longPreferencesKey(name = "fundsFoundDate")

    val userWasInteractWithRatingKey: Preferences.Key<Boolean> =
        booleanPreferencesKey(name = "userWasInteractWithRating")

    val usedCardsInfoKey: Preferences.Key<String> = stringPreferencesKey(name = "usedCardsInfo_v2")

    val usedeskClientIdKey: Preferences.Key<String> = stringPreferencesKey(name = "usedeskClientId")

    val appThemeModeKey: Preferences.Key<String> = stringPreferencesKey(name = "appThemeMode")

    val balanceHidingSettingsKey: Preferences.Key<String> = stringPreferencesKey(name = "balanceHidingSettings")

    val walletsScrollPreviewKey: Preferences.Key<Boolean> = booleanPreferencesKey(name = "walletsScrollPreview")

    val sentOneTimeEventsKey: Preferences.Key<String> = stringPreferencesKey(name = "sentOneTimeEvents")

    val walletsBalancesStatesKey: Preferences.Key<String> = stringPreferencesKey(name = "walletsBalancesStates")

    val wasTwinsOnboardingShownKey: Preferences.Key<Boolean> = booleanPreferencesKey(name = "twinsOnboardingShown")

    val wasApplicationStoppedKey: Preferences.Key<Boolean> = booleanPreferencesKey(name = "applicationStopped")

    val isTangemTosAcceptedKey: Preferences.Key<Boolean> = booleanPreferencesKey(name = "tangem_tos_accepted")

    val sendTapHelpPreviewKey: Preferences.Key<Boolean> = booleanPreferencesKey(name = "sendTapHelpPreview")

    val shouldOpenWelcomeOnResumeKey: Preferences.Key<Boolean> = booleanPreferencesKey(name = "openWelcomeOnResume")

    val shouldSaveAccessCodesKey: Preferences.Key<Boolean> = booleanPreferencesKey(name = "saveAccessCodes")

    val requireAccessCodeKey: Preferences.Key<Boolean> = booleanPreferencesKey(name = "requireAccessCode")

    val useBiometricAuthenticationKey: Preferences.Key<Boolean> =
        booleanPreferencesKey(name = "useBiometricAuthentication")

    val shouldShowMarketsTooltipKey: Preferences.Key<Boolean> = booleanPreferencesKey(name = "shouldShowMarketsTooltip")

    val walletFirstUsageDateKey: Preferences.Key<Long> = longPreferencesKey(name = "walletFirstUsageDate")

    val isWalletNamesMigrationDoneKey: Preferences.Key<Boolean> =
        booleanPreferencesKey(name = "isWalletNamesMigrationDone")

    val addedWalletsWithRingKey: Preferences.Key<Set<String>> = stringSetPreferencesKey(name = "addedWalletsWithRing")

    val geoUserCountryCodeKey: Preferences.Key<String> = stringPreferencesKey(name = "geoUserCountryCode")

    val onboardingFinalizeScanResponseKey: Preferences.Key<String> =
        stringPreferencesKey(name = "onboardingFinalizeScanResponse")

    val isGoogleServicesAvailableKey: Preferences.Key<Boolean> =
        booleanPreferencesKey(name = "isGoogleServicesAvailable")

    val isGooglePayAvailableKey: Preferences.Key<Boolean> = booleanPreferencesKey(name = "isGooglePayAvailable")

    val walletsNftEnabledStatesKey: Preferences.Key<String> = stringPreferencesKey(name = "walletsNftEnabledStates")

    val accessCodeSkippedStatesKey: Preferences.Key<String> = stringPreferencesKey(name = "accessCodeSkippedStates")

    val shouldShowUpgradeBannerKey: Preferences.Key<String> = stringPreferencesKey(name = "shouldShowUpgradeBanner")

    val upgradeBannerClosureTimestampKey: Preferences.Key<String> =
        stringPreferencesKey(name = "upgradeBannerClosureTimestamp")

    val walletCreationTimestampKey: Preferences.Key<String> = stringPreferencesKey(name = "walletCreationTimestamp")

    val hasHadFirstTopUpKey: Preferences.Key<String> = stringPreferencesKey(name = "hasHadFirstTopUp")

    val pendingAssetsDiscoveryKey: Preferences.Key<String> = stringPreferencesKey(name = "pendingAssetsDiscovery")

    val notificationsApplicationIdKey: Preferences.Key<String> =
        stringPreferencesKey(name = "notificationsApplicationId")

    val notificationsEnabledStatesKey: Preferences.Key<String> =
        stringPreferencesKey(name = "notificationsEnabledStates")

    val notificationsAutomaticallyEnabledStatesKey: Preferences.Key<String> =
        stringPreferencesKey(name = "notificationsAutomaticallyEnabledStates")

    val notificationsUserAllowSendAddressesKey: Preferences.Key<Boolean> =
        booleanPreferencesKey(name = "userAllowSendAddresses")

    val pushNotificationFirstActivationDoneWalletIdsKey: Preferences.Key<Set<String>> =
        stringSetPreferencesKey(name = "pushNotificationFirstActivationDoneWalletIds")

    val tronNetworkFeeNotificationShowCountKey: Preferences.Key<Int> =
        intPreferencesKey(name = "tronNetworkFeeNotificationShowCount")

    fun getShouldShowNotificationKey(key: String): Preferences.Key<Boolean> =
        booleanPreferencesKey("showShowNotificationUM_$key")

    fun getShouldShowPermission(permission: String): Preferences.Key<Boolean> =
        booleanPreferencesKey("shouldShowPushPermission_$permission")

    fun getShouldShowAskNotificationPermissionViaBs(): Preferences.Key<Boolean> =
        booleanPreferencesKey("ShouldShowAskNotificationPermissionViaBs")

    fun getShouldShowInitialPermissionScreen(permission: String): Preferences.Key<Boolean> =
        booleanPreferencesKey("shouldShowInitialPushPermissionScreen_$permission")

    fun getHotWalletUnlockAttemptsKey(attemptId: String): Preferences.Key<Int> =
        intPreferencesKey(name = "hotWalletUnlockAttempts_$attemptId")

    fun getHotWalletUnlockBootKey(attemptId: String): Preferences.Key<Int> =
        intPreferencesKey(name = "hotWalletUnlockBootCount_$attemptId")

    fun getHotWalletUnlockDeadlineKey(attemptId: String): Preferences.Key<Long> =
        longPreferencesKey(name = "hotWalletUnlockDeadline_$attemptId")
}