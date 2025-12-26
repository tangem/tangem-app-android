package com.tangem.features.feed.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.feed.model.feed.FeedComponentModel
import com.tangem.features.feed.model.market.details.MarketsTokenDetailsModel
import com.tangem.features.feed.model.market.list.MarketsListModel
import com.tangem.features.feed.model.news.details.NewsDetailsModel
import com.tangem.features.feed.model.news.list.NewsListModel
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

    @Binds
    @IntoMap
    @ClassKey(MarketsTokenDetailsModel::class)
    fun provideMarketsTokenDetailsModel(model: MarketsTokenDetailsModel): Model

    @Binds
    @IntoMap
    @ClassKey(NewsDetailsModel::class)
    fun provideNewsDetailsModel(model: NewsDetailsModel): Model

    @Binds
    @IntoMap
    @ClassKey(NewsListModel::class)
    fun provideNewsListModel(model: NewsListModel): Model
}