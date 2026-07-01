package com.tangem.tap.di.domain

import com.tangem.domain.addressbook.crypto.AddressBookCipher
import com.tangem.domain.addressbook.time.DefaultIsoTimestampProvider
import com.tangem.domain.addressbook.time.IsoTimestampProvider
import com.tangem.domain.addressbook.usecase.ValidateContactAddressUseCase
import com.tangem.domain.addressbook.usecase.VerifyAddressEntriesUseCase
import com.tangem.domain.tokens.GetNetworkAddressesUseCase
import com.tangem.domain.transaction.usecase.ValidateWalletAddressUseCase
import com.tangem.domain.transaction.usecase.VerifySecp256k1MessagesUseCase
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

    @Provides
    @Singleton
    fun provideVerifyAddressEntriesUseCase(
        verifyMessagesUseCase: VerifySecp256k1MessagesUseCase,
    ): VerifyAddressEntriesUseCase {
        return VerifyAddressEntriesUseCase(verifyMessagesUseCase = verifyMessagesUseCase)
    }

    @Provides
    @Singleton
    fun provideAddressBookCipher(): AddressBookCipher = AddressBookCipher()

    @Provides
    @Singleton
    fun provideIsoTimestampProvider(): IsoTimestampProvider = DefaultIsoTimestampProvider()
}