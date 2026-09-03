package com.tangem.tap.di.domain

import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.repository.JointAccountSupportedNetworksRepository
import com.tangem.domain.jointaccount.signing.JointAccountSigner
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.ActivateJointAccountUseCase
import com.tangem.domain.jointaccount.usecase.CreateJointAccountUseCase
import com.tangem.domain.jointaccount.usecase.GetJointAccountInvitePreviewUseCase
import com.tangem.domain.jointaccount.usecase.GetJointAccountInvitesUseCase
import com.tangem.domain.jointaccount.usecase.GetJointAccountSupportedNetworksUseCase
import com.tangem.domain.jointaccount.usecase.JoinJointAccountUseCase
import com.tangem.tap.domain.jointaccount.DefaultJointAccountSigner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object JointAccountDomainModule {

    @Provides
    @Singleton
    fun provideGetJointAccountSupportedNetworksUseCase(
        repository: JointAccountSupportedNetworksRepository,
    ): GetJointAccountSupportedNetworksUseCase {
        return GetJointAccountSupportedNetworksUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetJointAccountInvitesUseCase(repository: JointAccountRepository): GetJointAccountInvitesUseCase {
        return GetJointAccountInvitesUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideJointAccountSigner(signer: DefaultJointAccountSigner): JointAccountSigner = signer

    @Provides
    @Singleton
    fun provideCreateJointAccountUseCase(
        repository: JointAccountRepository,
        signer: JointAccountSigner,
        fetcher: SingleJointAccountListFetcher,
        supplier: SingleJointAccountListSupplier,
    ): CreateJointAccountUseCase {
        return CreateJointAccountUseCase(
            repository = repository,
            signer = signer,
            fetcher = fetcher,
            supplier = supplier,
        )
    }

    @Provides
    @Singleton
    fun provideGetJointAccountInvitePreviewUseCase(
        repository: JointAccountRepository,
    ): GetJointAccountInvitePreviewUseCase {
        return GetJointAccountInvitePreviewUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideActivateJointAccountUseCase(
        repository: JointAccountRepository,
        signer: JointAccountSigner,
        fetcher: SingleJointAccountListFetcher,
        supplier: SingleJointAccountListSupplier,
    ): ActivateJointAccountUseCase {
        return ActivateJointAccountUseCase(
            repository = repository,
            signer = signer,
            fetcher = fetcher,
            supplier = supplier,
        )
    }

    @Provides
    @Singleton
    fun provideJoinJointAccountUseCase(
        repository: JointAccountRepository,
        signer: JointAccountSigner,
        fetcher: SingleJointAccountListFetcher,
        supplier: SingleJointAccountListSupplier,
    ): JoinJointAccountUseCase {
        return JoinJointAccountUseCase(
            repository = repository,
            signer = signer,
            fetcher = fetcher,
            supplier = supplier,
        )
    }
}