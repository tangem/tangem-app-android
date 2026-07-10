package com.tangem.data.virtualaccount.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.data.virtualaccount.converter.VirtualAccountStatusValueDMConverter
import com.tangem.data.virtualaccount.flow.DefaultVirtualAccountStatusFetcher
import com.tangem.data.virtualaccount.flow.DefaultVirtualAccountStatusProducer
import com.tangem.data.virtualaccount.repository.DefaultVirtualAccountActivationRepository
import com.tangem.data.virtualaccount.store.VirtualAccountStatusesStore
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.visa.entity.VirtualAccountStatusValueDM
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.mapWithStringKeyTypes
import com.tangem.domain.virtualaccount.flow.VirtualAccountStatusFetcher
import com.tangem.domain.virtualaccount.flow.VirtualAccountStatusProducer
import com.tangem.domain.virtualaccount.flow.VirtualAccountStatusSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.virtualaccount.repository.VirtualAccountActivationRepository
import com.tangem.domain.virtualaccount.usecase.ActivateVirtualAccountUseCase
import com.tangem.domain.virtualaccount.usecase.GetVirtualAccountEligibilityUseCase
import com.tangem.domain.virtualaccount.usecase.GetVirtualAccountSuitableWalletsUseCase
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface VirtualAccountDataModule {

    @Binds
    @Singleton
    fun bindVirtualAccountStatusProducerFactory(
        impl: DefaultVirtualAccountStatusProducer.Factory,
    ): VirtualAccountStatusProducer.Factory

    @Binds
    @Singleton
    fun bindVirtualAccountStatusFetcher(impl: DefaultVirtualAccountStatusFetcher): VirtualAccountStatusFetcher

    @Binds
    @Singleton
    fun bindVirtualAccountActivationRepository(
        impl: DefaultVirtualAccountActivationRepository,
    ): VirtualAccountActivationRepository

    companion object {

        @Provides
        @Singleton
        fun provideVirtualAccountStatusesStore(
            @NetworkMoshi moshi: Moshi,
            @ApplicationContext context: Context,
            scope: AppCoroutineScope,
            converter: VirtualAccountStatusValueDMConverter,
        ): VirtualAccountStatusesStore {
            return VirtualAccountStatusesStore(
                runtimeStore = RuntimeSharedStore(),
                persistenceDataStore = DataStoreFactory.create(
                    serializer = MoshiDataStoreSerializer(
                        moshi = moshi,
                        types = mapWithStringKeyTypes<VirtualAccountStatusValueDM>(),
                        defaultValue = emptyMap(),
                    ),
                    corruptionHandler = ReplaceFileCorruptionHandler { emptyMap() },
                    produceFile = { context.dataStoreFile(fileName = "virtual_account_statuses") },
                    scope = scope,
                ),
                converter = converter,
                scope = scope,
            )
        }

        @Provides
        @Singleton
        fun provideVirtualAccountStatusSupplier(
            factory: VirtualAccountStatusProducer.Factory,
        ): VirtualAccountStatusSupplier {
            return object : VirtualAccountStatusSupplier(
                factory = factory,
                keyCreator = { "virtual_account_status_${it.userWalletId.stringValue}" },
            ) {}
        }

        @Provides
        @Singleton
        fun provideActivateVirtualAccountUseCase(
            repository: VirtualAccountActivationRepository,
        ): ActivateVirtualAccountUseCase {
            return ActivateVirtualAccountUseCase(repository = repository)
        }

        @Provides
        @Singleton
        fun provideGetVirtualAccountSuitableWalletsUseCase(
            userWalletsListRepository: UserWalletsListRepository,
        ): GetVirtualAccountSuitableWalletsUseCase {
            return GetVirtualAccountSuitableWalletsUseCase(userWalletsListRepository = userWalletsListRepository)
        }

        @Provides
        fun provideGetVirtualAccountEligibilityUseCase(
            getVirtualAccountSuitableWalletsUseCase: GetVirtualAccountSuitableWalletsUseCase,
            onboardingRepository: OnboardingRepository,
            deviceSecurityInfoProvider: DeviceSecurityInfoProvider,
        ): GetVirtualAccountEligibilityUseCase {
            return GetVirtualAccountEligibilityUseCase(
                getVirtualAccountSuitableWalletsUseCase = getVirtualAccountSuitableWalletsUseCase,
                onboardingRepository = onboardingRepository,
                deviceSecurityInfoProvider = deviceSecurityInfoProvider,
            )
        }
    }
}