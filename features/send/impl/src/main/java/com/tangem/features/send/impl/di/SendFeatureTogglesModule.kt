package com.tangem.features.send.impl.di

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.features.send.api.featuretoggles.SendFeatureToggles
import com.tangem.features.send.impl.featuretoggles.DefaultSendFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI module provides implementation of [SendFeatureToggles]
 */
@Module
@InstallIn(SingletonComponent::class)
internal object SendFeatureTogglesModule {

    @Provides
    @Singleton
    fun provideSendFeatureToggles(
        featureTogglesManager: FeatureTogglesManager,
        tangemTechApi: TangemTechApi,
        dispatchers: CoroutineDispatcherProvider,
    ): SendFeatureToggles {
        return DefaultSendFeatureToggles(
            featureTogglesManager = featureTogglesManager,
            tangemTechApi = tangemTechApi,
            dispatchers = dispatchers,
        )
    }
}