package com.tangem.features.survey.impl.di

import com.tangem.features.survey.SurveyComponent
import com.tangem.features.survey.SurveyFeatureToggles
import com.tangem.features.survey.SurveySparrowLauncher
import com.tangem.features.survey.deeplink.SurveyDeepLinkHandler
import com.tangem.features.survey.impl.DefaultSurveyComponent
import com.tangem.features.survey.impl.DefaultSurveyFeatureToggles
import com.tangem.features.survey.impl.DefaultSurveySparrowLauncher
import com.tangem.features.survey.impl.deeplink.DefaultSurveyDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SurveyModule {

    @Binds
    @Singleton
    fun bindSurveyFeatureToggles(impl: DefaultSurveyFeatureToggles): SurveyFeatureToggles

    @Binds
    @Singleton
    fun bindSurveySparrowLauncher(impl: DefaultSurveySparrowLauncher): SurveySparrowLauncher

    @Binds
    @Singleton
    fun bindSurveyComponentFactory(impl: DefaultSurveyComponent.Factory): SurveyComponent.Factory

    @Binds
    @Singleton
    fun bindSurveyDeepLinkHandlerFactory(impl: DefaultSurveyDeepLinkHandler.Factory): SurveyDeepLinkHandler.Factory
}