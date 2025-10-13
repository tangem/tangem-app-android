package com.tangem.domain.account.status.di

import com.tangem.domain.account.status.usecase.GetAccountCurrencyByAddressUseCase
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AccountStatusUseCaseModule {

    @Provides
    @Singleton
    fun provideGetAccountCurrencyByAddressUseCase(
        userWalletsListRepository: UserWalletsListRepository,
        multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
        singleAccountListSupplier: SingleAccountListSupplier,
    ): GetAccountCurrencyByAddressUseCase {
        return GetAccountCurrencyByAddressUseCase(
            userWalletsListRepository = userWalletsListRepository,
            multiNetworkStatusSupplier = multiNetworkStatusSupplier,
            singleAccountListSupplier = singleAccountListSupplier,
        )
    }

    @Provides
    @Singleton
    fun provideGetAccountCurrencyStatusUseCase(): GetAccountCurrencyStatusUseCase {
        return GetAccountCurrencyStatusUseCase()
    }
}