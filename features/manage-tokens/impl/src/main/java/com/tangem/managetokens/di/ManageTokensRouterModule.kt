package com.tangem.managetokens.di

import com.tangem.features.managetokens.navigation.ManageTokensUi
import com.tangem.managetokens.presentation.router.ManageTokensUiImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ManageTokensRouterModule {

    @Binds
    @Singleton
    fun provideManageTokensRouter(manageTokensUiImpl: ManageTokensUiImpl): ManageTokensUi
}
