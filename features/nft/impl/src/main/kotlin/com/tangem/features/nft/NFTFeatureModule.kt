package com.tangem.features.nft

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.decompose.model.Model
import com.tangem.features.nft.collections.DefaultNFTCollectionsComponent
import com.tangem.features.nft.collections.model.NFTCollectionsModel
import com.tangem.features.nft.component.NFTCollectionsComponent
import com.tangem.features.nft.component.NFTDetailsComponent
import com.tangem.features.nft.component.NFTReceiveComponent
import com.tangem.features.nft.details.DefaultNFTDetailsComponent
import com.tangem.features.nft.details.model.NFTDetailsModel
import com.tangem.features.nft.receive.DefaultNFTReceiveComponent
import com.tangem.features.nft.receive.model.NFTReceiveModel
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
internal object NFTFeatureModule {

    @Provides
    @Singleton
    fun provideFeatureToggles(featureTogglesManager: FeatureTogglesManager): NFTFeatureToggles {
        return DefaultNFTFeatureToggles(featureTogglesManager)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface NFTFeatureModuleBinds {
    @Binds
    @Singleton
    fun bindNFTCollectionsComponentFactory(
        impl: DefaultNFTCollectionsComponent.Factory,
    ): NFTCollectionsComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(NFTCollectionsModel::class)
    fun bindNFTCollectionsModel(model: NFTCollectionsModel): Model

    @Binds
    @Singleton
    fun bindNFTReceiveComponentFactory(impl: DefaultNFTReceiveComponent.Factory): NFTReceiveComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(NFTReceiveModel::class)
    fun bindNFTReceiveModel(model: NFTReceiveModel): Model

    @Binds
    @Singleton
    fun bindNFTDetailsComponentFactory(impl: DefaultNFTDetailsComponent.Factory): NFTDetailsComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(NFTDetailsModel::class)
    fun bindNFTDetailsModel(model: NFTDetailsModel): Model
}