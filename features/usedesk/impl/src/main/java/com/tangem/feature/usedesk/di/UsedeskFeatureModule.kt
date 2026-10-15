package com.tangem.feature.usedesk.di

import com.tangem.core.decompose.model.Model
import com.tangem.feature.usedesk.DefaultUsedeskComponent
import com.tangem.feature.usedesk.api.SupportChatVisibilityProvider
import com.tangem.feature.usedesk.api.UsedeskComponent
import com.tangem.feature.usedesk.model.UsedeskModel
import com.tangem.feature.usedesk.visibility.DefaultSupportChatVisibility
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface UsedeskFeatureModule {

    @Binds
    fun bindComponentFactory(impl: DefaultUsedeskComponent.Factory): UsedeskComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(UsedeskModel::class)
    fun bindModel(model: UsedeskModel): Model

    @Binds
    fun bindSupportChatVisibilityProvider(impl: DefaultSupportChatVisibility): SupportChatVisibilityProvider
}