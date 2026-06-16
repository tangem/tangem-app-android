package com.tangem.features.commonfeatures.impl.addtoportfolio.di

import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioComponent
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.impl.addtoportfolio.DefaultAddToPortfolioComponent
import com.tangem.features.commonfeatures.impl.addtoportfolio.ui.DefaultAddToPortfolioManager
import com.tangem.features.commonfeatures.impl.userportfolio.DefaultUserPortfolioComponent
import com.tangem.features.commonfeatures.impl.userportfolio.UserPortfolioComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AddToPortfolioComponentModule {

    @Binds
    fun bindAddToPortfolioComponent(factory: DefaultAddToPortfolioComponent.Factory): AddToPortfolioComponent.Factory

    @Binds
    fun bindAddToPortfolioManagerFactory(factory: DefaultAddToPortfolioManager.Factory): AddToPortfolioManager.Factory

    @Binds
    fun bindUserPortfolioComponentFactory(
        factory: DefaultUserPortfolioComponent.Factory,
    ): UserPortfolioComponent.Factory
}