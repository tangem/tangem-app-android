package com.tangem.datasource.di

import com.tangem.blockchain.common.AccountCreator
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.blockchain.DefaultAccountCreator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AccountCreatorModule {

    @Provides
    @Singleton
    fun provideAccountCreator(authProvider: AuthProvider, tangemTechApi: TangemTechApi): AccountCreator {
        return DefaultAccountCreator(authProvider, tangemTechApi)
    }
}
