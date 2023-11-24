package com.tangem.tap.di.domain

import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal object TransactionDomainModule {

    @Provides
    @ViewModelScoped
    fun provideGetUseCase(
        walletManagersFacade: WalletManagersFacade,
        dispatchers: CoroutineDispatcherProvider,
    ): GetFeeUseCase {
        return GetFeeUseCase(walletManagersFacade, dispatchers)
    }
}
