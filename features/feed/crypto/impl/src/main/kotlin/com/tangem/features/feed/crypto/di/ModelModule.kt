package com.tangem.features.feed.crypto.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.feed.crypto.model.CryptoFeedTabModel
import com.tangem.features.feed.crypto.model.search.CryptoFeedSearchTabModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface ModelModule {

    @Binds
    @IntoMap
    @ClassKey(CryptoFeedTabModel::class)
    fun bindCryptoFeedTabModel(model: CryptoFeedTabModel): Model

    @Binds
    @IntoMap
    @ClassKey(CryptoFeedSearchTabModel::class)
    fun bindCryptoFeedSearchTabModel(model: CryptoFeedSearchTabModel): Model
}