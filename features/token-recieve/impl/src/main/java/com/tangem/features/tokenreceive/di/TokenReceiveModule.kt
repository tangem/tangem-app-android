package com.tangem.features.tokenreceive.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.tokenreceive.DefaultTokenReceiveFeatureToggle
import com.tangem.features.tokenreceive.TokenReceiveComponent
import com.tangem.features.tokenreceive.TokenReceiveFeatureToggle
import com.tangem.features.tokenreceive.component.DefaultTokenReceiveComponent
import com.tangem.features.tokenreceive.model.TokenReceiveAssetsModel
import com.tangem.features.tokenreceive.model.TokenReceiveModel
import com.tangem.features.tokenreceive.model.TokenReceiveQrCodeModel
import com.tangem.features.tokenreceive.model.TokenReceiveWarningModel
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
internal object FeatureToggleModule {

    @Provides
    @Singleton
    fun provideTokenReceiveFeatureToggle(featureTogglesManager: FeatureTogglesManager): TokenReceiveFeatureToggle {
        return DefaultTokenReceiveFeatureToggle(
            featureTogglesManager = featureTogglesManager,
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface ComponentModule {

    @Binds
    @Singleton
    fun bindComponent(factory: DefaultTokenReceiveComponent.Factory): TokenReceiveComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface ModelModule {

    @Binds
    @IntoMap
    @ClassKey(TokenReceiveModel::class)
    fun bindsTokenReceiveModel(model: TokenReceiveModel): Model

    @Binds
    @IntoMap
    @ClassKey(TokenReceiveAssetsModel::class)
    fun bindsTokenReceiveAssetsModel(model: TokenReceiveAssetsModel): Model

    @Binds
    @IntoMap
    @ClassKey(TokenReceiveQrCodeModel::class)
    fun bindsTokenReceiveQrCodeModel(model: TokenReceiveQrCodeModel): Model

    @Binds
    @IntoMap
    @ClassKey(TokenReceiveWarningModel::class)
    fun bindsTokenReceiveWarningModel(model: TokenReceiveWarningModel): Model
}