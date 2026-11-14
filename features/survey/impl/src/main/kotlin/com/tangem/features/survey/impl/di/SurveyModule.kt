package com.tangem.features.survey.impl.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.survey.SurveyComponent
import com.tangem.features.survey.SurveyFeatureToggles
import com.tangem.features.survey.SurveySparrowLauncher
import com.tangem.features.survey.impl.DefaultSurveyComponent
import com.tangem.features.survey.impl.DefaultSurveyFeatureToggles
import com.tangem.features.survey.impl.DefaultSurveySparrowLauncher
import com.tangem.features.survey.impl.deeplink.SurveyDeepLinkHandler
import com.tangem.features.survey.impl.nativesurvey.repository.DefaultNativeSurveyRepository
import com.tangem.features.survey.impl.nativesurvey.repository.NativeSurveyRepository
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
    @IntoMap
    @DeepLinkKey(SurveyDeepLinkHandler.DEEPLINK_KEY)
    fun bindSurveyDeepLinkHandler(impl: SurveyDeepLinkHandler): DeepLinkHandler

    @Binds
    @Singleton
    fun bindNativeSurveyRepository(impl: DefaultNativeSurveyRepository): NativeSurveyRepository

    companion object {

        @Provides
        @Singleton
        fun provideSurveyFeatureToggles(featureTogglesManager: FeatureTogglesManager): SurveyFeatureToggles {
            return DefaultSurveyFeatureToggles(featureTogglesManager)
        }
    }
}