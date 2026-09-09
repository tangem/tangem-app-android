package com.tangem.tap.di.domain

import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.jointaccount.blockaid.JointOperationBlockaidGate
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.gas.JointRefundReceiverProvider
import com.tangem.domain.jointaccount.gas.SafeTxGasBuilder
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.repository.JointAccountSupportedNetworksRepository
import com.tangem.domain.jointaccount.safe.SafeCompositionChecker
import com.tangem.domain.jointaccount.signing.JointAccountSigner
import com.tangem.domain.jointaccount.signing.JointOperationSigner
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.ActivateJointAccountUseCase
import com.tangem.domain.jointaccount.usecase.CreateJointAccountUseCase
import com.tangem.domain.jointaccount.usecase.CreateJointOperationUseCase
import com.tangem.domain.jointaccount.usecase.GetJointAccountInvitePreviewUseCase
import com.tangem.domain.jointaccount.usecase.GetJointAccountInvitesUseCase
import com.tangem.domain.jointaccount.usecase.GetJointAccountSupportedNetworksUseCase
import com.tangem.domain.jointaccount.usecase.JoinJointAccountUseCase
import com.tangem.domain.jointaccount.usecase.SignJointOperationUseCase
import com.tangem.domain.jointaccount.verifier.JointOperationVerifier
import com.tangem.domain.transaction.GaslessTransactionRepository
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
    fun provideJointRefundReceiverProvider(
        gaslessTransactionRepository: GaslessTransactionRepository,
    ): JointRefundReceiverProvider {
        return JointRefundReceiverProvider(gaslessTransactionRepository::getTokenFeeReceiverAddress)
    }

    @Provides
    @Singleton
    fun provideSafeTxGasBuilder(
        networkReader: JointNetworkReader,
        refundReceiverProvider: JointRefundReceiverProvider,
    ): SafeTxGasBuilder {
        return SafeTxGasBuilder(
            networkReader = networkReader,
            refundReceiverProvider = refundReceiverProvider,
        )
    }

    @Provides
    @Singleton
    fun provideSafeCompositionChecker(networkReader: JointNetworkReader): SafeCompositionChecker {
        return SafeCompositionChecker(networkReader = networkReader)
    }

    @Provides
    @Singleton
    fun provideCreateJointOperationUseCase(
        repository: JointAccountRepository,
        signer: JointOperationSigner,
        fetcher: SingleJointAccountListFetcher,
        supplier: SingleJointAccountListSupplier,
        gasBuilder: SafeTxGasBuilder,
        compositionChecker: SafeCompositionChecker,
        networkReader: JointNetworkReader,
    ): CreateJointOperationUseCase {
        return CreateJointOperationUseCase(
            repository = repository,
            signer = signer,
            fetcher = fetcher,
            supplier = supplier,
            gasBuilder = gasBuilder,
            compositionChecker = compositionChecker,
            networkReader = networkReader,
        )
    }

    @Provides
    @Singleton
    fun provideJointOperationVerifier(): JointOperationVerifier = JointOperationVerifier()

    @Provides
    @Singleton
    fun provideSignJointOperationUseCase(
        repository: JointAccountRepository,
        signer: JointOperationSigner,
        fetcher: SingleJointAccountListFetcher,
        supplier: SingleJointAccountListSupplier,
        verifier: JointOperationVerifier,
        blockaidGate: JointOperationBlockaidGate,
        ownerAddressResolver: JointOwnerAddressResolver,
        networkReader: JointNetworkReader,
    ): SignJointOperationUseCase {
        return SignJointOperationUseCase(
            repository = repository,
            signer = signer,
            fetcher = fetcher,
            supplier = supplier,
            verifier = verifier,
            blockaidGate = blockaidGate,
            ownerAddressResolver = ownerAddressResolver,
            networkReader = networkReader,
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