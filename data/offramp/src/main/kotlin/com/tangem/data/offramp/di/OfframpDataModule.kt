package com.tangem.data.offramp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.tangem.core.local.datastore.KotlinxDataStoreSerializer
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.data.offramp.DefaultOfframpRepository
import com.tangem.data.offramp.MoonPayCredentials
import com.tangem.data.offramp.model.PendingOfframpEntry
import com.tangem.domain.offramp.GetOfframpUrlUseCase
import com.tangem.domain.offramp.repository.OfframpRepository
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.grow.datasource.config.GrowEnvironmentConfig
import com.tangem.grow.datasource.moonpay.MoonPayApi
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.builtins.ListSerializer

@Module
@InstallIn(SingletonComponent::class)
internal object OfframpDataModule {

    @Provides
    @Singleton
    fun providePendingOfframpStore(
        @ApplicationContext context: Context,
        appScope: AppCoroutineScope,
    ): DataStore<List<PendingOfframpEntry>> = DataStoreFactory.create(
        serializer = KotlinxDataStoreSerializer(
            defaultValue = emptyList(),
            serializer = ListSerializer(PendingOfframpEntry.serializer()),
        ),
        produceFile = { context.dataStoreFile(fileName = "pending_offramps") },
        scope = appScope,
    )

    @Provides
    @Singleton
    fun provideOfframpRepository(
        api: MoonPayApi,
        growEnvironmentConfig: GrowEnvironmentConfig,
        getSelectedWalletUseCase: GetSelectedWalletUseCase,
        appThemeModeHolder: AppThemeModeHolder,
        pendingOfframpStore: DataStore<List<PendingOfframpEntry>>,
        dispatchers: CoroutineDispatcherProvider,
    ): OfframpRepository {
        return DefaultOfframpRepository(
            api = api,
            credentials = MoonPayCredentials(
                apiKey = growEnvironmentConfig.moonPayApiKey,
                secretKey = growEnvironmentConfig.moonPayApiSecretKey,
            ),
            userWalletProvider = { getSelectedWalletUseCase.sync().getOrNull() },
            appThemeModeHolder = appThemeModeHolder,
            pendingOfframpStore = pendingOfframpStore,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideGetOfframpUrlUseCase(offrampRepository: OfframpRepository): GetOfframpUrlUseCase {
        return GetOfframpUrlUseCase(offrampRepository)
    }
}