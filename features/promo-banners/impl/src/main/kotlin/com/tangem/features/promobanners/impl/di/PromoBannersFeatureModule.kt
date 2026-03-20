package com.tangem.features.promobanners.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.features.promobanners.api.NewPromoBannersFeatureToggles
import com.tangem.features.promobanners.api.PromoBannersBlockComponent
import com.tangem.features.promobanners.impl.DefaultPromoBannersBlockComponent
import com.tangem.features.promobanners.impl.model.PromoBannersBlockModel
import com.tangem.features.promobanners.impl.repository.DefaultPromoBannersRepository
import com.tangem.features.promobanners.impl.repository.PromoBannersRepository
import com.tangem.features.promobanners.impl.toggles.DefaultNewPromoBannersFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
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
internal interface PromoBannersFeatureModule {

    @Binds
    @Singleton
    fun bindPromoBannersBlockComponentFactory(
        factory: DefaultPromoBannersBlockComponent.Factory,
    ): PromoBannersBlockComponent.Factory

    @Binds
    @Singleton
    fun bindFeatureToggles(impl: DefaultNewPromoBannersFeatureToggles): NewPromoBannersFeatureToggles

    companion object {

        @Provides
        @Singleton
        fun provideRepository(
            tangemTechApi: TangemTechApi,
            dispatchers: CoroutineDispatcherProvider,
        ): PromoBannersRepository {
            return DefaultPromoBannersRepository(
                tangemTechApi = tangemTechApi,
                dispatchers = dispatchers,
                cache = RuntimeSharedStore(),
            )
        }
    }
}

@Module
@InstallIn(ModelComponent::class)
internal interface PromoBannersModelModule {

    @Binds
    @IntoMap
    @ClassKey(PromoBannersBlockModel::class)
    fun bindPromoBannersBlockModel(model: PromoBannersBlockModel): Model
}