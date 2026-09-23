package com.tangem.datasource.remote.auth.di

import com.tangem.core.remote.header.CardAuthHeaderProvider
import com.tangem.core.remote.header.TangemApiKeyHeaderProvider
import com.tangem.datasource.remote.auth.AuthProvider
import com.tangem.datasource.remote.header.AuthenticationHeader
import com.tangem.datasource.remote.header.TangemApiKeyHeader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object AuthHeaderProvidersModule {

    @Provides
    fun provideTangemApiKeyHeaderProvider(authProvider: AuthProvider): TangemApiKeyHeaderProvider {
        return TangemApiKeyHeaderProvider { environment ->
            TangemApiKeyHeader(authProvider, environment)
        }
    }

    @Provides
    fun provideCardAuthHeaderProvider(authProvider: AuthProvider): CardAuthHeaderProvider {
        return CardAuthHeaderProvider { AuthenticationHeader(authProvider) }
    }
}