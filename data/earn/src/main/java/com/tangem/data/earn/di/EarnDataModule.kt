package com.tangem.data.earn.di

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.data.earn.DefaultEarnErrorResolver
import com.tangem.data.earn.datastore.EarnFilterStore
import com.tangem.data.earn.datastore.EarnNetworksStore
import com.tangem.data.earn.datastore.EarnTokensCatalogStore
import com.tangem.data.earn.datastore.EarnTopTokensStore
import com.tangem.data.earn.repository.DefaultEarnRepository
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.earn.EarnErrorResolver
import com.tangem.domain.earn.repository.EarnRepository
import com.tangem.utils.annotations.RemoveWithToggle
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object EarnDataModule {

    @Provides
    fun provideEarnErrorResolver(): EarnErrorResolver = DefaultEarnErrorResolver()

    @Provides
    @Singleton
    @RemoveWithToggle(
        toggleName = "TWI_1520_MERKL_INCENTIVES_ENABLED",
        description = "Drop isMerklIncentivesEnabled from DefaultEarnRepository and always send GROSS_APY_TYPE.",
    )
    fun provideEarnRepository(
        tangemTechApi: TangemTechApi,
        dispatchers: CoroutineDispatcherProvider,
        userWalletsListRepository: UserWalletsListRepository,
        earnNetworksStore: EarnNetworksStore,
        earnTopTokensStore: EarnTopTokensStore,
        earnTokensCatalogStore: EarnTokensCatalogStore,
        earnFilterStore: EarnFilterStore,
        earnErrorResolver: EarnErrorResolver,
        excludedBlockchains: ExcludedBlockchains,
        featureTogglesManager: FeatureTogglesManager,
    ): EarnRepository {
        return DefaultEarnRepository(
            tangemTechApi = tangemTechApi,
            dispatchers = dispatchers,
            userWalletsListRepository = userWalletsListRepository,
            earnNetworksStore = earnNetworksStore,
            earnTopTokensStore = earnTopTokensStore,
            earnTokensCatalogStore = earnTokensCatalogStore,
            earnErrorResolver = earnErrorResolver,
            excludedBlockchains = excludedBlockchains,
            earnFilterStore = earnFilterStore,
            isMerklIncentivesEnabled = featureTogglesManager.isFeatureEnabled(
                FeatureToggles.TWI_1520_MERKL_INCENTIVES_ENABLED,
            ),
        )
    }
}