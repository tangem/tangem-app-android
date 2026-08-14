package com.tangem.data.polymarket.di

import android.content.Context
import com.tangem.common.services.secure.SecureStorage
import com.tangem.data.polymarket.cleaner.PolymarketUserWalletDataCleaner
import com.tangem.data.polymarket.store.DefaultPolymarketCredentialsStore
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.sdk.storage.AndroidSecureStorageV2
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PolymarketStorageModule {

    @Provides
    @Singleton
    fun providePolymarketCredentialsStore(
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
    ): PolymarketCredentialsStore {
        val secureStorage: SecureStorage = AndroidSecureStorageV2(
            appContext = context,
            useStrongBox = false,
            name = "polymarket_credentials_storage",
        )

        return DefaultPolymarketCredentialsStore(
            secureStorage = secureStorage,
            json = Json { ignoreUnknownKeys = true },
            dispatchers = dispatchers,
        )
    }

    @Provides
    @IntoSet
    fun providePolymarketUserWalletDataCleaner(impl: PolymarketUserWalletDataCleaner): UserWalletDataCleaner = impl
}