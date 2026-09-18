package com.tangem.features.feed.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.feed.components.v2.blocks.model.AddAccountBlockModel
import com.tangem.features.feed.components.v2.blocks.model.CollectiblesBlockModel
import com.tangem.features.feed.components.v2.blocks.model.EarnBlockModel
import com.tangem.features.feed.components.v2.blocks.model.PredictionsBlockModel
import com.tangem.features.feed.components.v2.blocks.model.ReferralBlockModel
import com.tangem.features.feed.components.v2.blocks.model.TangemPayBlockModel
import com.tangem.features.feed.components.v2.model.FeedHomeModel
import com.tangem.features.feed.model.FeedEntryModel
import com.tangem.features.feed.model.earn.EarnModel
import com.tangem.features.feed.model.earn.filters.EarnNetworkFilterModel
import com.tangem.features.feed.model.earn.filters.EarnTypeFilterModel
import com.tangem.features.feed.model.feed.FeedComponentModel
import com.tangem.features.feed.model.market.details.MarketsTokenDetailsModel
import com.tangem.features.feed.model.market.list.MarketsListModel
import com.tangem.features.feed.model.news.details.NewsDetailsModel
import com.tangem.features.feed.model.news.list.NewsListModel
import com.tangem.features.feed.model.search.SearchModel
import com.tangem.features.feed.model.search.SearchTokenSelectorModel
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
    @ClassKey(EarnModel::class)
    fun bindsEarnModel(model: EarnModel): Model

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

    @Binds
    @IntoMap
    @ClassKey(FeedEntryModel::class)
    fun provideFeedEntryModel(model: FeedEntryModel): Model

    @Binds
    @IntoMap
    @ClassKey(EarnNetworkFilterModel::class)
    fun provideEarnNetworkFilterModel(model: EarnNetworkFilterModel): Model

    @Binds
    @IntoMap
    @ClassKey(EarnTypeFilterModel::class)
    fun provideEarnTypeFilterModel(model: EarnTypeFilterModel): Model

    @Binds
    @IntoMap
    @ClassKey(SearchModel::class)
    fun provideSearchModel(model: SearchModel): Model

    @Binds
    @IntoMap
    @ClassKey(SearchTokenSelectorModel::class)
    fun provideSearchTokenSelectorModel(model: SearchTokenSelectorModel): Model

    @Binds
    @IntoMap
    @ClassKey(FeedHomeModel::class)
    fun provideFeedHomeModel(model: FeedHomeModel): Model

    @Binds
    @IntoMap
    @ClassKey(AddAccountBlockModel::class)
    fun provideAddAccountBlockModel(model: AddAccountBlockModel): Model

    @Binds
    @IntoMap
    @ClassKey(EarnBlockModel::class)
    fun provideEarnBlockModel(model: EarnBlockModel): Model

    @Binds
    @IntoMap
    @ClassKey(ReferralBlockModel::class)
    fun provideReferralBlockModel(model: ReferralBlockModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayBlockModel::class)
    fun provideTangemPayBlockModel(model: TangemPayBlockModel): Model

    @Binds
    @IntoMap
    @ClassKey(PredictionsBlockModel::class)
    fun providePredictionsBlockModel(model: PredictionsBlockModel): Model

    @Binds
    @IntoMap
    @ClassKey(CollectiblesBlockModel::class)
    fun provideCollectiblesBlockModel(model: CollectiblesBlockModel): Model
}