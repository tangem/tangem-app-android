package com.tangem.tap.di.domain

import com.tangem.domain.addressbook.crypto.AddressBookCipher
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.addressbook.time.DefaultIsoTimestampProvider
import com.tangem.domain.addressbook.time.IsoTimestampProvider
import com.tangem.domain.addressbook.usecase.CreateContactUseCase
import com.tangem.domain.addressbook.usecase.DeleteContactUseCase
import com.tangem.domain.addressbook.usecase.GetContactsUseCase
import com.tangem.domain.addressbook.usecase.GetVerifiedContactsUseCase
import com.tangem.domain.addressbook.usecase.SignAddressEntriesUseCase
import com.tangem.domain.addressbook.usecase.UpdateContactUseCase
import com.tangem.domain.addressbook.usecase.ValidateContactAddressUseCase
import com.tangem.domain.addressbook.usecase.ValidateContactNameUseCase
import com.tangem.domain.addressbook.usecase.VerifyAddressEntriesUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.tokens.GetNetworkAddressesUseCase
import com.tangem.domain.transaction.usecase.SignUseCase
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
    fun provideSignAddressEntriesUseCase(signUseCase: SignUseCase): SignAddressEntriesUseCase {
        return SignAddressEntriesUseCase(signUseCase = signUseCase)
    }

    @Provides
    @Singleton
    fun provideValidateContactNameUseCase(repository: AddressBookRepository): ValidateContactNameUseCase {
        return ValidateContactNameUseCase(repository = repository)
    }

    @Provides
    @Singleton
    fun provideGetContactsUseCase(repository: AddressBookRepository): GetContactsUseCase {
        return GetContactsUseCase(repository = repository)
    }

    @Provides
    @Singleton
    fun provideGetVerifiedContactsUseCase(
        getContactsUseCase: GetContactsUseCase,
        verifyAddressEntriesUseCase: VerifyAddressEntriesUseCase,
        userWalletsListRepository: UserWalletsListRepository,
    ): GetVerifiedContactsUseCase {
        return GetVerifiedContactsUseCase(
            getContacts = getContactsUseCase,
            verifyAddressEntries = verifyAddressEntriesUseCase,
            userWalletsListRepository = userWalletsListRepository,
        )
    }

    @Provides
    @Singleton
    fun provideCreateContactUseCase(
        repository: AddressBookRepository,
        validateContactNameUseCase: ValidateContactNameUseCase,
        signAddressEntriesUseCase: SignAddressEntriesUseCase,
        timestampProvider: IsoTimestampProvider,
    ): CreateContactUseCase {
        return CreateContactUseCase(
            repository = repository,
            validateContactName = validateContactNameUseCase,
            signAddressEntries = signAddressEntriesUseCase,
            timestampProvider = timestampProvider,
        )
    }

    @Provides
    @Singleton
    fun provideUpdateContactUseCase(
        repository: AddressBookRepository,
        signAddressEntriesUseCase: SignAddressEntriesUseCase,
        timestampProvider: IsoTimestampProvider,
    ): UpdateContactUseCase {
        return UpdateContactUseCase(
            repository = repository,
            signAddressEntries = signAddressEntriesUseCase,
            timestampProvider = timestampProvider,
        )
    }

    @Provides
    @Singleton
    fun provideDeleteContactUseCase(repository: AddressBookRepository): DeleteContactUseCase {
        return DeleteContactUseCase(repository = repository)
    }

    @Provides
    @Singleton
    fun provideAddressBookCipher(): AddressBookCipher = AddressBookCipher()

    @Provides
    @Singleton
    fun provideIsoTimestampProvider(): IsoTimestampProvider = DefaultIsoTimestampProvider()
}