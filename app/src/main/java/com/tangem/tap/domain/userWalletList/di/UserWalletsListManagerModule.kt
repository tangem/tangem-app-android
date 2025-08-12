package com.tangem.tap.domain.userWalletList.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.common.authentication.storage.AuthenticatedStorage
import com.tangem.common.json.TangemSdkAdapter
import com.tangem.common.services.secure.SecureStorage
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.models.scan.serialization.*
import com.tangem.domain.visa.model.VisaActivationRemoteState
import com.tangem.domain.visa.model.VisaCardActivationStatus
import com.tangem.domain.wallets.hot.HotWalletPasswordRequester
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.sdk.storage.AndroidSecureStorage
import com.tangem.sdk.storage.AndroidSecureStorageV2
import com.tangem.sdk.storage.createEncryptedSharedPreferences
import com.tangem.tap.domain.userWalletList.implementation.BiometricUserWalletsListManager
import com.tangem.tap.domain.userWalletList.implementation.GeneralUserWalletsListManager
import com.tangem.tap.domain.userWalletList.implementation.RuntimeUserWalletsListManager
import com.tangem.tap.domain.userWalletList.repository.DefaultUserWalletsListRepository
import com.tangem.tap.domain.userWalletList.repository.DelegatedKeystoreManager
import com.tangem.tap.domain.userWalletList.repository.UserWalletEncryptionKeysRepository
import com.tangem.tap.domain.userWalletList.repository.UserWalletsKeysStoreDecorator
import com.tangem.tap.domain.userWalletList.repository.implementation.BiometricUserWalletsKeysRepository
import com.tangem.tap.domain.userWalletList.repository.implementation.DefaultSelectedUserWalletRepository
import com.tangem.tap.domain.userWalletList.repository.implementation.DefaultUserWalletsPublicInformationRepository
import com.tangem.tap.domain.userWalletList.repository.implementation.DefaultUserWalletsSensitiveInformationRepository
import com.tangem.tap.tangemSdkManager
import com.tangem.utils.Provider
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object UserWalletsListManagerModule {

    @Provides
    @Singleton
    @Deprecated("Use UserWalletsListRepository instead")
    fun provideGeneralUserWalletsListManager(
        @ApplicationContext applicationContext: Context,
        appPreferencesStore: AppPreferencesStore,
        dispatchers: CoroutineDispatcherProvider,
        analyticsEventHandler: AnalyticsEventHandler,
    ): UserWalletsListManager {
        return GeneralUserWalletsListManager(
            runtimeUserWalletsListManager = RuntimeUserWalletsListManager(),
            biometricUserWalletsListManager = createBiometricUserWalletsListManager(
                applicationContext = applicationContext,
                analyticsEventHandler = analyticsEventHandler,
                dispatchers = dispatchers,
            ),
            appPreferencesStore = appPreferencesStore,
            dispatchers = dispatchers,
        )
    }

    @Deprecated("Use UserWalletsListRepository instead")
    private fun createBiometricUserWalletsListManager(
        applicationContext: Context,
        analyticsEventHandler: AnalyticsEventHandler,
        dispatchers: CoroutineDispatcherProvider,
    ): UserWalletsListManager {
        val moshi = buildMoshi()
        val secureStorage = buildSecureStorage(applicationContext = applicationContext)

        val authenticatedStorage = AuthenticatedStorage(
            secureStorage = UserWalletsKeysStoreDecorator(
                featureStorage = secureStorage,
                cardSdkStorageProvider = Provider { tangemSdkManager.secureStorage },
            ),
            keystoreManager = DelegatedKeystoreManager(
                keystoreManagerProvider = Provider { tangemSdkManager.keystoreManager },
            ),
        )

        val keysRepository = BiometricUserWalletsKeysRepository(
            moshi = moshi,
            secureStorage = secureStorage,
            authenticatedStorage = authenticatedStorage,
            analyticsEventHandler = analyticsEventHandler,
        )

        val publicInformationRepository = DefaultUserWalletsPublicInformationRepository(
            moshi = moshi,
            secureStorage = secureStorage,
        )

        val sensitiveInformationRepository = DefaultUserWalletsSensitiveInformationRepository(
            moshi = moshi,
            secureStorage = secureStorage,
        )

        val selectedUserWalletRepository = DefaultSelectedUserWalletRepository(
            secureStorage = secureStorage,
            dispatchers = dispatchers,
        )

        return BiometricUserWalletsListManager(
            keysRepository = keysRepository,
            publicInformationRepository = publicInformationRepository,
            sensitiveInformationRepository = sensitiveInformationRepository,
            selectedUserWalletRepository = selectedUserWalletRepository,
        )
    }

    @Provides
    @Singleton
    fun provideUserWalletsListRepository(
        @ApplicationContext applicationContext: Context,
        dispatchers: CoroutineDispatcherProvider,
        passwordRequester: HotWalletPasswordRequester,
    ): UserWalletsListRepository {
        val moshi = buildMoshi()
        val secureStorage = buildSecureStorage(applicationContext = applicationContext)

        val authenticatedStorage = AuthenticatedStorage(
            secureStorage = UserWalletsKeysStoreDecorator(
                featureStorage = secureStorage,
                cardSdkStorageProvider = Provider { tangemSdkManager.secureStorage },
            ),
            keystoreManager = DelegatedKeystoreManager(
                keystoreManagerProvider = Provider { tangemSdkManager.keystoreManager },
            ),
        )

        val publicInformationRepository = DefaultUserWalletsPublicInformationRepository(
            moshi = moshi,
            secureStorage = secureStorage,
        )

        val sensitiveInformationRepository = DefaultUserWalletsSensitiveInformationRepository(
            moshi = moshi,
            secureStorage = secureStorage,
        )

        val selectedUserWalletRepository = DefaultSelectedUserWalletRepository(
            secureStorage = secureStorage,
            dispatchers = dispatchers,
        )

        val userWalletEncryptionKeysRepository = UserWalletEncryptionKeysRepository(
            moshi = moshi,
            authenticatedStorage = authenticatedStorage,
            dispatchers = dispatchers,
            secureStorage = secureStorage,
        )

        return DefaultUserWalletsListRepository(
            publicInformationRepository = publicInformationRepository,
            sensitiveInformationRepository = sensitiveInformationRepository,
            selectedUserWalletRepository = selectedUserWalletRepository,
            passwordRequester = passwordRequester,
            userWalletEncryptionKeysRepository = userWalletEncryptionKeysRepository,
            tangemSdkManagerProvider = Provider { tangemSdkManager },
            savePersistentInformation = ProviderSuspend { true }, // Always save persistent information for now
            // TODO add a settings toggle to disable saving persistent information
        )
    }

    fun buildMoshi(): Moshi {
        return Moshi.Builder()
            .add(WalletDerivedKeysMapAdapter())
            .add(ScanResponseDerivedKeysMapAdapter())
            .add(ByteArrayKeyAdapter())
            .add(ExtendedPublicKeysMapAdapter())
            .add(CardBackupStatusAdapter())
            .add(DerivationPathAdapterWithMigration())
            .add(TangemSdkAdapter.DateAdapter())
            .add(TangemSdkAdapter.DerivationNodeAdapter())
            .add(TangemSdkAdapter.FirmwareVersionAdapter()) // For PrimaryCard model
            .add(VisaActivationRemoteState.jsonAdapter)
            .add(VisaCardActivationStatus.jsonAdapter)
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    fun buildSecureStorage(@ApplicationContext applicationContext: Context): SecureStorage {
        return AndroidSecureStorage(
            preferences = SecureStorage.createEncryptedSharedPreferences(
                context = applicationContext,
                storageName = "user_wallets_storage",
            ),
            androidSecureStorageV2 = AndroidSecureStorageV2(
                appContext = applicationContext,
                useStrongBox = true,
                name = "user_wallets_storage2",
            ),
            androidSecureStorageV3 = AndroidSecureStorageV2(
                appContext = applicationContext,
                useStrongBox = false,
                name = "user_wallets_storage3",
            ),
        )
    }
}