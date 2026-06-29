package com.tangem.tap.di.domain

import com.tangem.domain.addressbook.crypto.AddressBookCipher
import com.tangem.domain.addressbook.interactor.GetVerifiedContactsInteractor
import com.tangem.domain.addressbook.interactor.SaveContactInteractor
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.addressbook.time.DefaultIsoTimestampProvider
import com.tangem.domain.addressbook.time.IsoTimestampProvider
import com.tangem.domain.addressbook.usecase.DeleteContactUseCase
import com.tangem.domain.addressbook.usecase.GetContactsUseCase
import com.tangem.domain.addressbook.usecase.SyncAddressBooksUseCase
import com.tangem.domain.addressbook.usecase.ValidateContactAddressUseCase
import com.tangem.domain.addressbook.usecase.ValidateContactNameUseCase
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
    fun provideGetVerifiedContactsInteractor(
        getContactsUseCase: GetContactsUseCase,
        verifyMessagesUseCase: VerifySecp256k1MessagesUseCase,
        userWalletsListRepository: UserWalletsListRepository,
    ): GetVerifiedContactsInteractor {
        return GetVerifiedContactsInteractor(
            getContacts = getContactsUseCase,
            verifyMessages = verifyMessagesUseCase,
            userWalletsListRepository = userWalletsListRepository,
        )
    }

    @Provides
    @Singleton
    fun provideSaveContactInteractor(
        repository: AddressBookRepository,
        validateContactNameUseCase: ValidateContactNameUseCase,
        signUseCase: SignUseCase,
        timestampProvider: IsoTimestampProvider,
    ): SaveContactInteractor {
        return SaveContactInteractor(
            repository = repository,
            validateContactName = validateContactNameUseCase,
            signUseCase = signUseCase,
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
    fun provideSyncAddressBooksUseCase(repository: AddressBookRepository): SyncAddressBooksUseCase {
        return SyncAddressBooksUseCase(repository = repository)
    }

    @Provides
    @Singleton
    fun provideAddressBookCipher(): AddressBookCipher = AddressBookCipher()

    @Provides
    @Singleton
    fun provideIsoTimestampProvider(): IsoTimestampProvider = DefaultIsoTimestampProvider()
}