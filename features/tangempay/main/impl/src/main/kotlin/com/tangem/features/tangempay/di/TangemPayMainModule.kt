package com.tangem.features.tangempay.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.tangempay.DefaultTangemPayMainEventListener
import com.tangem.features.tangempay.component.DefaultTangemPayMainBannerComponent
import com.tangem.features.tangempay.TangemPayMainBannerComponent
import com.tangem.features.tangempay.TangemPayMainEventListener
import com.tangem.features.tangempay.model.TangemPayMainBannerModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TangemPayMainModule {
    @Binds
    fun bindTangemPayMainBannerComponent(
        factory: DefaultTangemPayMainBannerComponent.Factory,
    ): TangemPayMainBannerComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(TangemPayMainBannerModel::class)
    fun bindTangemPayMainBannerModel(model: TangemPayMainBannerModel): Model

    @Binds
    @Singleton
    fun bindTangemPayMainEventListener(impl: DefaultTangemPayMainEventListener): TangemPayMainEventListener
}