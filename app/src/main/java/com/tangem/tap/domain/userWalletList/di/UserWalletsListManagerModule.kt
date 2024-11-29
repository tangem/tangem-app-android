package com.tangem.tap.domain.userWalletList.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.common.authentication.storage.AuthenticatedStorage
import com.tangem.common.json.TangemSdkAdapter
import com.tangem.common.services.secure.SecureStorage
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.models.scan.serialization.*
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.sdk.storage.AndroidSecureStorage
import com.tangem.sdk.storage.createEncryptedSharedPreferences
import com.tangem.tap.domain.userWalletList.implementation.BiometricUserWalletsListManager
import com.tangem.tap.domain.userWalletList.implementation.GeneralUserWalletsListManager
import com.tangem.tap.domain.userWalletList.implementation.RuntimeUserWalletsListManager
import com.tangem.tap.domain.userWalletList.repository.DelegatedKeystoreManager
import com.tangem.tap.domain.userWalletList.repository.UserWalletsKeysStoreDecorator
import com.tangem.tap.domain.userWalletList.repository.implementation.BiometricUserWalletsKeysRepository
import com.tangem.tap.domain.userWalletList.repository.implementation.DefaultSelectedUserWalletRepository
import com.tangem.tap.domain.userWalletList.repository.implementation.DefaultUserWalletsPublicInformationRepository
import com.tangem.tap.domain.userWalletList.repository.implementation.DefaultUserWalletsSensitiveInformationRepository
import com.tangem.tap.tangemSdkManager
import com.tangem.utils.Provider
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
    fun provideGeneralUserWalletsListManager(
        @ApplicationContext applicationContext: Context,
        appPreferencesStore: AppPreferencesStore,
        dispatchers: CoroutineDispatcherProvider,
    ): UserWalletsListManager {
        return GeneralUserWalletsListManager(
            runtimeUserWalletsListManager = RuntimeUserWalletsListManager(),
            biometricUserWalletsListManager = createBiometricUserWalletsListManager(applicationContext),
            appPreferencesStore = appPreferencesStore,
            dispatchers = dispatchers,
        )
    }

    private fun createBiometricUserWalletsListManager(applicationContext: Context): UserWalletsListManager {
        val moshi = Moshi.Builder()
            .add(WalletDerivedKeysMapAdapter())
            .add(ScanResponseDerivedKeysMapAdapter())
            .add(ByteArrayKeyAdapter())
            .add(ExtendedPublicKeysMapAdapter())
            .add(CardBackupStatusAdapter())
            .add(DerivationPathAdapterWithMigration())
            .add(TangemSdkAdapter.DateAdapter())
            .add(TangemSdkAdapter.DerivationNodeAdapter())
            .add(TangemSdkAdapter.FirmwareVersionAdapter()) // For PrimaryCard model
            .add(KotlinJsonAdapterFactory())
            .build()

        val secureStorage = AndroidSecureStorage(
            preferences = SecureStorage.createEncryptedSharedPreferences(
                context = applicationContext,
                storageName = "user_wallets_storage",
            ),
        )

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
        )

        val publicInformationRepository = DefaultUserWalletsPublicInformationRepository(
            moshi = moshi,
            secureStorage = secureStorage,
        )

        val sensitiveInformationRepository = DefaultUserWalletsSensitiveInformationRepository(
            moshi = moshi,
            secureStorage = secureStorage,
        )

        val selectedUserWalletRepository = DefaultSelectedUserWalletRepository(secureStorage = secureStorage)

        return BiometricUserWalletsListManager(
            keysRepository = keysRepository,
            publicInformationRepository = publicInformationRepository,
            sensitiveInformationRepository = sensitiveInformationRepository,
            selectedUserWalletRepository = selectedUserWalletRepository,
        )
    }
}
