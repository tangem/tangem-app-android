package com.tangem.datasource.di

import com.tangem.core.remote.header.CardAuthHeaderProvider
import com.tangem.core.remote.header.TangemApiKeyHeaderProvider
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.utils.AuthenticationHeader
import com.tangem.datasource.utils.TangemApiKeyHeader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object ApiConfigsModule {

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