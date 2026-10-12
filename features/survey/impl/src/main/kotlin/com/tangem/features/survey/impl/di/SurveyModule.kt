package com.tangem.features.survey.impl.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.features.survey.SurveyComponent
import com.tangem.features.survey.SurveySparrowLauncher
import com.tangem.features.survey.deeplink.SurveyDeepLinkHandler
import com.tangem.features.survey.impl.DefaultSurveyComponent
import com.tangem.features.survey.impl.DefaultSurveySparrowLauncher
import com.tangem.features.survey.impl.deeplink.DefaultSurveyDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SurveyModule {

    @Binds
    @Singleton
    fun bindSurveySparrowLauncher(impl: DefaultSurveySparrowLauncher): SurveySparrowLauncher

    @Binds
    @Singleton
    fun bindSurveyComponentFactory(impl: DefaultSurveyComponent.Factory): SurveyComponent.Factory

    @Binds
    @Singleton
    fun bindSurveyDeepLinkHandlerFactory(impl: DefaultSurveyDeepLinkHandler.Factory): SurveyDeepLinkHandler.Factory

    companion object {

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultSurveyDeepLinkHandler.DEEPLINK_KEY)
        fun provideSurveyDeepLinkHandler(factory: SurveyDeepLinkHandler.Factory): DeepLinkHandler =
            DeepLinkHandler { request -> factory.create(queryParams = request.queryParams) }
    }
}