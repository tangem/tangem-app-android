package com.tangem.tap.common.di.domain.wallets

import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object WalletsDomainModule {

    @Provides
    @ViewModelScoped
    fun providesGetWalletsUseCase(walletsStateHolder: WalletsStateHolder): GetWalletsUseCase {
        return GetWalletsUseCase(walletsStateHolder = walletsStateHolder)
    }
}
