package com.tangem.tap.di.domain

import com.tangem.domain.addressbook.usecase.ValidateContactAddressUseCase
import com.tangem.domain.tokens.GetNetworkAddressesUseCase
import com.tangem.domain.transaction.usecase.ValidateWalletAddressUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AddressBookDomainModule {

    @Provides
    @Singleton
    fun provideValidateContactAddressUseCase(
        validateWalletAddressUseCase: ValidateWalletAddressUseCase,
        getNetworkAddressesUseCase: GetNetworkAddressesUseCase,
    ): ValidateContactAddressUseCase {
        return ValidateContactAddressUseCase(
            validateWalletAddressUseCase = validateWalletAddressUseCase,
            getNetworkAddressesUseCase = getNetworkAddressesUseCase,
        )
    }
}