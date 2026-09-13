package com.tangem.features.feed.earn.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.feed.earn.model.EarnFeedTabModel
import com.tangem.features.feed.earn.model.filters.EarnNetworkFilterModel
import com.tangem.features.feed.earn.model.filters.EarnTypeFilterModel
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
    @ClassKey(EarnFeedTabModel::class)
    fun bindEarnFeedTabModel(model: EarnFeedTabModel): Model

    @Binds
    @IntoMap
    @ClassKey(EarnNetworkFilterModel::class)
    fun bindEarnNetworkFilterModel(model: EarnNetworkFilterModel): Model

    @Binds
    @IntoMap
    @ClassKey(EarnTypeFilterModel::class)
    fun bindEarnTypeFilterModel(model: EarnTypeFilterModel): Model
}