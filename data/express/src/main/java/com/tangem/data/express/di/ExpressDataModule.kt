package com.tangem.data.express.di

import com.squareup.moshi.Moshi
import com.tangem.data.express.DefaultExpressRepository
import com.tangem.data.express.converter.ExpressErrorConverter
import com.tangem.data.express.error.DefaultExpressErrorResolver
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.response.ExpressErrorResponse
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.express.ExpressErrorResolver
import com.tangem.domain.express.ExpressRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ExpressDataModule {

    @Provides
    @Singleton
    fun provideExpressErrorResolver(@NetworkMoshi moshi: Moshi): ExpressErrorResolver {
        val jsonAdapter = moshi.adapter(ExpressErrorResponse::class.java)
        return DefaultExpressErrorResolver(
            ExpressErrorConverter(jsonAdapter),
        )
    }

    @Provides
    @Singleton
    fun provideExpressRepository(
        tangemExpressApi: TangemExpressApi,
        appPreferencesStore: AppPreferencesStore,
        dispatchers: CoroutineDispatcherProvider,
    ): ExpressRepository {
        return DefaultExpressRepository(
            tangemExpressApi = tangemExpressApi,
            appPreferencesStore = appPreferencesStore,
            dispatchers = dispatchers,
        )
    }
}