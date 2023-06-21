package com.tangem.feature.learn2earn.domain.di

import android.content.Context
import com.tangem.feature.learn2earn.data.api.Learn2earnRepository
import com.tangem.feature.learn2earn.domain.DefaultLearn2earnInteractor
import com.tangem.feature.learn2earn.domain.api.Learn2earnInteractor
import com.tangem.feature.learn2earn.presentation.Learn2earnRouter
import com.tangem.lib.auth.BasicAuthProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class Learn2earnDomainModule {

    @Provides
    @Singleton
    fun provideRouter(@ApplicationContext context: Context): Learn2earnRouter {
        return Learn2earnRouter(context)
    }

    @Provides
    @Singleton
    fun provideInteractor(
        repository: Learn2earnRepository,
        basicAuthProvider: BasicAuthProvider,
    ): Learn2earnInteractor {
        return DefaultLearn2earnInteractor(
            repository = repository,
            basicAuthProvider = basicAuthProvider,
            // TODO: 1inch: paste locale provider instead of hardcode
            userCountryCodeProvider = { "ru" },
        )
    }
}