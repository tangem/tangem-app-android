package com.tangem.data.jointaccount.di

import android.content.Context
import androidx.datastore.dataStoreFile
import com.tangem.common.services.secure.SecureStorage
import com.tangem.core.local.datastore.KotlinxDataStoreSerializer
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.jointaccount.address.DefaultJointAccountSafeAddressResolver
import com.tangem.data.jointaccount.address.DefaultJointOwnerAddressResolver
import com.tangem.data.jointaccount.blockaid.DefaultJointOperationBlockaidGate
import com.tangem.data.jointaccount.DefaultJointAccountSupportedNetworksRepository
import com.tangem.data.jointaccount.cleaner.JointAccountUserWalletDataCleaner
import com.tangem.data.jointaccount.converter.JointAccountDMConverter
import com.tangem.data.jointaccount.derivation.DefaultJointOwnerKeysRepository
import com.tangem.data.jointaccount.fetcher.DefaultSingleJointAccountListFetcher
import com.tangem.data.jointaccount.producer.DefaultSingleJointAccountListProducer
import com.tangem.data.jointaccount.safe.DefaultJointTransferPayloadCodec
import com.tangem.data.jointaccount.safe.DefaultSafeAddressCalculator
import com.tangem.data.jointaccount.repository.DefaultJointAccountRepository
import com.tangem.data.jointaccount.store.DefaultJointAccountInvitesStore
import com.tangem.data.jointaccount.store.JointAccountDM
import com.tangem.data.jointaccount.store.JointAccountsStore
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.jointaccount.address.JointAccountSafeAddressResolver
import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.jointaccount.blockaid.JointOperationBlockaidGate
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.polling.JointAccountPollingCoordinator
import com.tangem.domain.jointaccount.repository.JointAccountSupportedNetworksRepository
import com.tangem.domain.jointaccount.repository.JointOwnerKeysRepository
import com.tangem.data.jointaccount.signing.DefaultJointOperationSigner
import com.tangem.domain.jointaccount.producer.SingleJointAccountListProducer
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.safe.JointTransferPayloadCodec
import com.tangem.domain.jointaccount.safe.SafeAddressCalculator
import com.tangem.domain.jointaccount.signing.JointOperationSigner
import com.tangem.domain.jointaccount.store.JointAccountInvitesStore
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.sdk.storage.AndroidSecureStorageV2
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface JointAccountDataModule {

    @Binds
    @Singleton
    fun bindJointTransferPayloadCodec(codec: DefaultJointTransferPayloadCodec): JointTransferPayloadCodec

    @Binds
    @Singleton
    fun bindSafeAddressCalculator(calculator: DefaultSafeAddressCalculator): SafeAddressCalculator

    @Binds
    @Singleton
    fun bindSingleJointAccountListProducerFactory(
        factory: DefaultSingleJointAccountListProducer.Factory,
    ): SingleJointAccountListProducer.Factory

    @Binds
    @Singleton
    fun bindSingleJointAccountListFetcher(fetcher: DefaultSingleJointAccountListFetcher): SingleJointAccountListFetcher

    @Binds
    @Singleton
    fun bindJointAccountRepository(repository: DefaultJointAccountRepository): JointAccountRepository

    @Binds
    @Singleton
    fun bindJointAccountSafeAddressResolver(
        resolver: DefaultJointAccountSafeAddressResolver,
    ): JointAccountSafeAddressResolver

    @Binds
    @Singleton
    fun bindJointAccountSupportedNetworksRepository(
        repository: DefaultJointAccountSupportedNetworksRepository,
    ): JointAccountSupportedNetworksRepository

    @Binds
    @Singleton
    fun bindJointOperationSigner(signer: DefaultJointOperationSigner): JointOperationSigner

    @Binds
    @Singleton
    fun bindJointOwnerAddressResolver(resolver: DefaultJointOwnerAddressResolver): JointOwnerAddressResolver

    @Binds
    @Singleton
    fun bindJointOwnerKeysRepository(repository: DefaultJointOwnerKeysRepository): JointOwnerKeysRepository

    @Binds
    @Singleton
    fun bindJointOperationBlockaidGate(gate: DefaultJointOperationBlockaidGate): JointOperationBlockaidGate

    companion object {

        @Provides
        @Singleton
        fun provideJointAccountsStore(
            @ApplicationContext context: Context,
            dataStoreFactory: AppDataStoreFactory,
            converter: JointAccountDMConverter,
            scope: AppCoroutineScope,
        ): JointAccountsStore {
            return JointAccountsStore(
                runtimeStore = RuntimeSharedStore(),
                persistenceDataStore = dataStoreFactory.create(
                    serializer = KotlinxDataStoreSerializer(
                        defaultValue = emptyMap(),
                        serializer = MapSerializer(
                            keySerializer = String.serializer(),
                            valueSerializer = ListSerializer(JointAccountDM.serializer()),
                        ),
                    ),
                    scope = scope,
                    produceFile = { context.dataStoreFile(fileName = "joint_accounts") },
                ),
                converter = converter,
                scope = scope,
            )
        }

        @Provides
        @Singleton
        fun provideJointAccountInvitesStore(
            @ApplicationContext context: Context,
            dispatchers: CoroutineDispatcherProvider,
        ): JointAccountInvitesStore {
            val secureStorage: SecureStorage = AndroidSecureStorageV2(
                appContext = context,
                useStrongBox = false,
                name = "joint_account_invites_storage",
            )

            return DefaultJointAccountInvitesStore(
                secureStorage = secureStorage,
                json = Json { ignoreUnknownKeys = true },
                dispatchers = dispatchers,
            )
        }

        @Provides
        @IntoSet
        fun provideJointAccountUserWalletDataCleaner(impl: JointAccountUserWalletDataCleaner): UserWalletDataCleaner {
            return impl
        }

        @Provides
        @Singleton
        fun provideSingleJointAccountListSupplier(
            factory: SingleJointAccountListProducer.Factory,
        ): SingleJointAccountListSupplier {
            return object : SingleJointAccountListSupplier(
                factory = factory,
                keyCreator = { params -> "joint_account_list_${params.userWalletId.stringValue}" },
            ) {}
        }

        @Provides
        @Singleton
        fun provideJointAccountPollingCoordinator(
            fetcher: SingleJointAccountListFetcher,
            appScope: AppCoroutineScope,
        ): JointAccountPollingCoordinator {
            return JointAccountPollingCoordinator(fetcher = fetcher, appScope = appScope)
        }
    }
}