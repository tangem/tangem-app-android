package com.tangem.features.onramp.tokenlist.di

import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onramp.tokenlist.model.OnrampTokenListModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(DecomposeComponent::class)
internal interface OnrampTokenListModelModule {

    @Binds
    @IntoMap
    @ClassKey(OnrampTokenListModel::class)
    fun bindOnrampTokenListModel(model: OnrampTokenListModel): Model
}