package com.tangem.feature.referral.di

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.feature.referral.converters.ReferralConverter
import com.tangem.feature.referral.data.DefaultMobileWalletPromoRepository
import com.tangem.feature.referral.data.ExternalReferralRepository
import com.tangem.feature.referral.data.ReferralRepositoryImpl
import com.tangem.feature.referral.domain.MobileWalletPromoRepository
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
        userWalletsListRepository: UserWalletsListRepository,
        dispatchers: CoroutineDispatcherProvider,
        excludedBlockchains: ExcludedBlockchains,
    ): ReferralRepository {
        return ReferralRepositoryImpl(
            referralApi = tangemTechApi,
            referralConverter = referralConverter,
            userWalletsListRepository = userWalletsListRepository,
            dispatchers = dispatchers,
            excludedBlockchains = excludedBlockchains,
        )
    }

    @Provides
    @Singleton
    fun provideExternalReferralRepository(
        tangemTechApi: TangemTechApi,
        referralConverter: ReferralConverter,
        userWalletsListRepository: UserWalletsListRepository,
        dispatchers: CoroutineDispatcherProvider,
        excludedBlockchains: ExcludedBlockchains,
    ): ExternalReferralRepository {
        return ReferralRepositoryImpl(
            referralApi = tangemTechApi,
            referralConverter = referralConverter,
            userWalletsListRepository = userWalletsListRepository,
            dispatchers = dispatchers,
            excludedBlockchains = excludedBlockchains,
        )
    }

    @Provides
    @Singleton
    fun provideMobileWalletPromoRepository(appPreferencesStore: AppPreferencesStore): MobileWalletPromoRepository =
        DefaultMobileWalletPromoRepository(
            appPreferencesStore = appPreferencesStore,
        )
}