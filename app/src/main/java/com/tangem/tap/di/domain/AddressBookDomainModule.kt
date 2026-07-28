package com.tangem.tap.di.domain

import com.tangem.domain.addressbook.crypto.AddressBookCipher
import com.tangem.domain.addressbook.interactor.GetVerifiedContactsInteractor
import com.tangem.domain.addressbook.interactor.SaveContactInteractor
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.addressbook.time.DefaultIsoTimestampProvider
import com.tangem.domain.addressbook.time.IsoTimestampProvider
import com.tangem.domain.addressbook.usecase.*
import com.tangem.domain.addressbook.validation.ContactNameValidator
import com.tangem.domain.addressbook.verification.ContactSignatureVerifier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.transaction.usecase.SignUseCase
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
    fun provideContactSignatureVerifier(
        verifyMessagesUseCase: VerifySecp256k1MessagesUseCase,
        userWalletsListRepository: UserWalletsListRepository,
    ): ContactSignatureVerifier {
        return ContactSignatureVerifier(
            verifyMessages = verifyMessagesUseCase,
            userWalletsListRepository = userWalletsListRepository,
        )
    }

    @Provides
    @Singleton
    fun provideContactNameValidator(
        repository: AddressBookRepository,
        contactSignatureVerifier: ContactSignatureVerifier,
    ): ContactNameValidator {
        return ContactNameValidator(
            repository = repository,
            contactSignatureVerifier = contactSignatureVerifier,
        )
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
        contactSignatureVerifier: ContactSignatureVerifier,
    ): GetVerifiedContactsInteractor {
        return GetVerifiedContactsInteractor(
            getContacts = getContactsUseCase,
            contactSignatureVerifier = contactSignatureVerifier,
        )
    }

    @Provides
    @Singleton
    fun provideSaveContactInteractor(
        repository: AddressBookRepository,
        contactNameValidator: ContactNameValidator,
        signUseCase: SignUseCase,
        timestampProvider: IsoTimestampProvider,
    ): SaveContactInteractor {
        return SaveContactInteractor(
            repository = repository,
            validateContactName = contactNameValidator,
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
    fun provideGetContactByIdUseCase(
        repository: AddressBookRepository,
        contactSignatureVerifier: ContactSignatureVerifier,
    ): GetContactByIdUseCase {
        return GetContactByIdUseCase(
            repository = repository,
            contactSignatureVerifier = contactSignatureVerifier,
        )
    }

    @Provides
    @Singleton
    fun provideCheckAddressDuplicateUseCase(
        repository: AddressBookRepository,
        contactSignatureVerifier: ContactSignatureVerifier,
    ): CheckAddressDuplicateUseCase {
        return CheckAddressDuplicateUseCase(
            repository = repository,
            contactSignatureVerifier = contactSignatureVerifier,
        )
    }

    @Provides
    @Singleton
    fun provideSyncAddressBooksUseCase(repository: AddressBookRepository): SyncAddressBooksUseCase {
        return SyncAddressBooksUseCase(repository = repository)
    }

    @Provides
    @Singleton
    fun provideIsAddressBookCompatibleUseCase(repository: AddressBookRepository): IsAddressBookCompatibleUseCase {
        return IsAddressBookCompatibleUseCase(repository = repository)
    }

    @Provides
    @Singleton
    fun provideAddressBookCipher(): AddressBookCipher = AddressBookCipher()

    @Provides
    @Singleton
    fun provideIsoTimestampProvider(): IsoTimestampProvider = DefaultIsoTimestampProvider()
}