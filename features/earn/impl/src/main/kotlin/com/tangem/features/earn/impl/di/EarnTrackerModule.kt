package com.tangem.features.earn.impl.di

import com.tangem.features.earn.impl.tracker.DefaultEarnTxTracker
import com.tangem.features.earn.product.api.session.EarnTxTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface EarnTrackerModule {

    @Binds
    @Singleton
    fun bindEarnTxTracker(tracker: DefaultEarnTxTracker): EarnTxTracker
}