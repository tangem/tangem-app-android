package com.tangem.features.introduction.impl.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.introduction.IntroductionComponent
import com.tangem.features.introduction.IntroductionFeatureToggles
import com.tangem.features.introduction.impl.DefaultIntroductionComponent
import com.tangem.features.introduction.impl.DefaultIntroductionFeatureToggles
import com.tangem.features.introduction.impl.engine.DefaultIntroductionVideoPlayerFactory
import com.tangem.features.introduction.impl.engine.IntroductionVideoPlayer
import com.tangem.features.introduction.impl.model.IntroductionModel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface IntroductionComponentModule {

    @Binds
    @Singleton
    fun bindIntroductionComponentFactory(factory: DefaultIntroductionComponent.Factory): IntroductionComponent.Factory

    @Binds
    @Singleton
    fun bindIntroductionVideoPlayerFactory(
        factory: DefaultIntroductionVideoPlayerFactory,
    ): IntroductionVideoPlayer.Factory
}

@Module
@InstallIn(SingletonComponent::class)
internal object IntroductionFeatureTogglesModule {

    @Provides
    @Singleton
    fun provideIntroductionFeatureToggles(featureTogglesManager: FeatureTogglesManager): IntroductionFeatureToggles {
        return DefaultIntroductionFeatureToggles(featureTogglesManager = featureTogglesManager)
    }
}

@Module
@InstallIn(ModelComponent::class)
internal interface IntroductionModelModule {

    @Binds
    @IntoMap
    @ClassKey(IntroductionModel::class)
    fun bindIntroductionModel(model: IntroductionModel): Model
}