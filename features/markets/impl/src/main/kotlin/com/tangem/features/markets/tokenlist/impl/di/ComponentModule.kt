package com.tangem.features.markets.tokenlist.impl.di

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.markets.tokenlist.MarketsTokenListComponent
import com.tangem.features.markets.tokenlist.impl.DefaultMarketsTokenListComponent
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ComponentModule {

    @Binds
    @Singleton
    fun bindMarketsTokenListBottomSheetComponent(
        factory: DefaultMarketsTokenListComponent.FactoryBottomSheet,
    ): MarketsTokenListComponent.FactoryBottomSheet
}

@Module
@InstallIn(SingletonComponent::class)
internal class ComponentProvideModule {

    @Provides
    @Singleton
    fun providesMarketsTokenListScreenComponent(
        factory: DefaultMarketsTokenListComponent.FactoryBottomSheet,
    ): MarketsTokenListComponent.FactoryScreen {
        return object : MarketsTokenListComponent.FactoryScreen {
            override fun create(context: AppComponentContext, params: Unit): MarketsTokenListComponent {
                return factory.create(context, params, null)
            }
        }
    }
}