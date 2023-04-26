package com.tangem.data.source.card.di

import com.tangem.data.source.card.CardDataSource
import com.tangem.data.source.card.DefaultCardDataSource
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Suppress("unused") // TODO: Remove in https://tangem.atlassian.net/browse/AND-3253
@Module
interface CardDataSourceModule {

    @Binds
    @Singleton
    fun bindDefaultCardDataSource(source: DefaultCardDataSource): CardDataSource
}
