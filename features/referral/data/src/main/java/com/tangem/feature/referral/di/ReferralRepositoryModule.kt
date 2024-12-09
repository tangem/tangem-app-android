package com.tangem.feature.referral.di

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.demo.DemoModeDatasource
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.feature.referral.converters.ReferralConverter
import com.tangem.feature.referral.data.ReferralRepositoryImpl
import com.tangem.feature.referral.domain.ReferralRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ReferralRepositoryModule {

    @Provides
    @Singleton
    fun provideReferralRepository(
        tangemTechApi: TangemTechApi,
        referralConverter: ReferralConverter,
        coroutineDispatcherProvider: CoroutineDispatcherProvider,
        demoModeDatasource: DemoModeDatasource,
        userWalletsStore: UserWalletsStore,
        excludedBlockchains: ExcludedBlockchains,
    ): ReferralRepository {
        return ReferralRepositoryImpl(
            referralApi = tangemTechApi,
            referralConverter = referralConverter,
            coroutineDispatcher = coroutineDispatcherProvider,
            demoModeDatasource = demoModeDatasource,
            userWalletsStore = userWalletsStore,
            excludedBlockchains = excludedBlockchains,
        )
    }
}