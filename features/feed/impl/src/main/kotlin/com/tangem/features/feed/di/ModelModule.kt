package com.tangem.features.feed.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.feed.model.feed.FeedComponentModel
import com.tangem.features.feed.model.market.list.MarketsListModel
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
    @ClassKey(FeedComponentModel::class)
    fun bindsFeedComponentModel(model: FeedComponentModel): Model

    @Binds
    @IntoMap
    @ClassKey(MarketsListModel::class)
    fun provideMarketsListModel(model: MarketsListModel): Model
}