package com.tangem.tap.di.domain

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.tangem.datasource.utils.KotlinxDataStoreSerializer
import com.tangem.domain.offramp.GetOfframpUrlUseCase
import com.tangem.domain.offramp.repository.OfframpRepository
import com.tangem.tap.data.DefaultOfframpRepository
import com.tangem.tap.data.model.PendingOfframpEntry
import com.tangem.tap.network.exchangeServices.SellService
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.builtins.ListSerializer

@Module
@InstallIn(SingletonComponent::class)
internal object OfframpDomainModule {

    @Provides
    @Singleton
    fun providePendingOfframpStore(
        @ApplicationContext context: Context,
        appScope: AppCoroutineScope,
    ): DataStore<List<PendingOfframpEntry>> = DataStoreFactory.create(
        serializer = KotlinxDataStoreSerializer(
            defaultValue = emptyList(),
            serializer = ListSerializer(PendingOfframpEntry.serializer()),
        ),
        produceFile = { context.dataStoreFile(fileName = "pending_offramps") },
        scope = appScope,
    )

    @Provides
    @Singleton
    fun provideOfframpRepository(
        sellService: SellService,
        pendingOfframpStore: DataStore<List<PendingOfframpEntry>>,
        dispatchers: CoroutineDispatcherProvider,
    ): OfframpRepository {
        return DefaultOfframpRepository(sellService, pendingOfframpStore, dispatchers)
    }

    @Provides
    @Singleton
    fun provideGetOfframpUrlUseCase(offrampRepository: OfframpRepository): GetOfframpUrlUseCase {
        return GetOfframpUrlUseCase(offrampRepository)
    }
}