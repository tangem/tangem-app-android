package com.tangem.store.datasource.di

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import com.tangem.datasource.appcurrency.DefaultAppCurrencyResponseStore
import com.tangem.datasource.local.preferences.migration.SharedPreferencesKeyMigrationFactory
import com.tangem.store.datasource.preferences.StorePreferencesKeys
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal object StorePreferencesMigrationsModule {

    @Provides
    @IntoSet
    fun provideSelectedAppCurrencyMigration(
        factory: SharedPreferencesKeyMigrationFactory,
    ): DataMigration<Preferences> {
        return factory.create(
            legacyPrefsName = "selected_app_currency",
            legacyKeyName = "key",
            keyName = DefaultAppCurrencyResponseStore.SELECTED_APP_CURRENCY_KEY.name,
        )
    }

    @Provides
    @IntoSet
    fun provideLegacyTapPrefsMigration(
        factory: SharedPreferencesKeyMigrationFactory,
        keys: StorePreferencesKeys,
    ): DataMigration<Preferences> {
        return factory.createBulk(
            legacyPrefsName = "tapPrefs",
            keyNames = setOf(
                keys.saveUserWalletsKey.name,
                keys.shouldShowAskBiometryKey.name,
                keys.appLaunchCountKey.name,
                keys.showRatingDialogAtLaunchCountKey.name,
                keys.fundsFoundDateKey.name,
                keys.userWasInteractWithRatingKey.name,
                keys.usedCardsInfoKey.name,
                keys.wasTwinsOnboardingShownKey.name,
                keys.isTangemTosAcceptedKey.name,
                keys.wasApplicationStoppedKey.name,
                keys.shouldOpenWelcomeOnResumeKey.name,
                keys.shouldSaveAccessCodesKey.name,
            ),
        )
    }

    @Provides
    @IntoSet
    fun provideAppThemeModeMigration(
        factory: SharedPreferencesKeyMigrationFactory,
        keys: StorePreferencesKeys,
    ): DataMigration<Preferences> {
        return factory.create(
            legacyPrefsName = "app_theme",
            legacyKeyName = "key",
            keyName = keys.appThemeModeKey.name,
        )
    }

    @Provides
    @IntoSet
    fun provideBalanceHidingMigration(
        factory: SharedPreferencesKeyMigrationFactory,
        keys: StorePreferencesKeys,
    ): DataMigration<Preferences> {
        return factory.create(
            legacyPrefsName = "balance_hiding_settings",
            legacyKeyName = "key",
            keyName = keys.balanceHidingSettingsKey.name,
        )
    }
}