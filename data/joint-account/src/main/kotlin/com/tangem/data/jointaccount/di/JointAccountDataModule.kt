package com.tangem.data.jointaccount.di

import android.content.Context
import androidx.datastore.dataStoreFile
import com.tangem.core.local.datastore.KotlinxDataStoreSerializer
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.jointaccount.converter.JointAccountDMConverter
import com.tangem.data.jointaccount.fetcher.DefaultSingleJointAccountListFetcher
import com.tangem.data.jointaccount.producer.DefaultSingleJointAccountListProducer
import com.tangem.data.jointaccount.store.JointAccountDM
import com.tangem.data.jointaccount.store.JointAccountsStore
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.producer.SingleJointAccountListProducer
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface JointAccountDataModule {

    @Binds
    @Singleton
    fun bindSingleJointAccountListProducerFactory(
        factory: DefaultSingleJointAccountListProducer.Factory,
    ): SingleJointAccountListProducer.Factory

    @Binds
    @Singleton
    fun bindSingleJointAccountListFetcher(fetcher: DefaultSingleJointAccountListFetcher): SingleJointAccountListFetcher

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
        fun provideSingleJointAccountListSupplier(
            factory: SingleJointAccountListProducer.Factory,
        ): SingleJointAccountListSupplier {
            return object : SingleJointAccountListSupplier(
                factory = factory,
                keyCreator = { params -> "joint_account_list_${params.userWalletId.stringValue}" },
            ) {}
        }
    }
}