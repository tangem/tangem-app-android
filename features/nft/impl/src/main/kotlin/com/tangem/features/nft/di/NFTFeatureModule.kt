package com.tangem.features.nft.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.decompose.model.Model
import com.tangem.features.nft.DefaultNFTFeatureToggles
import com.tangem.features.nft.NFTFeatureToggles
import com.tangem.features.nft.collections.model.NFTCollectionsModel
import com.tangem.features.nft.common.DefaultNFTComponent
import com.tangem.features.nft.component.*
import com.tangem.features.nft.details.block.DefaultNFTDetailsBlockComponent
import com.tangem.features.nft.details.info.DefaultNFTDetailsInfoComponent
import com.tangem.features.nft.details.info.NFTDetailsInfoComponent
import com.tangem.features.nft.details.model.NFTDetailsModel
import com.tangem.features.nft.receive.model.NFTReceiveModel
import com.tangem.features.nft.traits.model.NFTAssetTraitsModel
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
    fun bindNFTDetailsInfoComponentFactory(
        factory: DefaultNFTDetailsInfoComponent.Factory,
    ): NFTDetailsInfoComponent.Factory

    @Binds
    @Singleton
    fun bindNFTComponentFactory(impl: DefaultNFTComponent.Factory): NFTComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(NFTCollectionsModel::class)
    fun bindNFTCollectionsModel(model: NFTCollectionsModel): Model

    @Binds
    @IntoMap
    @ClassKey(NFTReceiveModel::class)
    fun bindNFTReceiveModel(model: NFTReceiveModel): Model

    @Binds
    @IntoMap
    @ClassKey(NFTDetailsModel::class)
    fun bindNFTDetailsModel(model: NFTDetailsModel): Model

    @Binds
    @Singleton
    fun bindNFTDetailsBlockComponentFactory(
        impl: DefaultNFTDetailsBlockComponent.Factory,
    ): NFTDetailsBlockComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(NFTAssetTraitsModel::class)
    fun bindNFTTraitsModel(model: NFTAssetTraitsModel): Model
}