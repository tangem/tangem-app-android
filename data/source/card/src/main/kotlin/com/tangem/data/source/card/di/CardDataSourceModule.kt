package com.tangem.data.source.card.di

import com.tangem.data.source.card.CardDataSource
import com.tangem.data.source.card.DefaultCardDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
internal interface CardDataSourceModule {

    @Binds
    @ActivityScoped
    fun bindDefaultCardDataSource(source: DefaultCardDataSource): CardDataSource
}
