package com.tangem.tap.legacy.card.di

import com.tangem.domain.card.CardTypeResolver
import com.tangem.domain.common.Provider
import com.tangem.tap.legacy.card.LegacyCardTypeResolver
import com.tangem.tap.proxy.AppStateHolder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object LegacyCardModule {

    @Provides
    @Singleton
    fun providesCardTypeResolver(reduxStateHolder: AppStateHolder): CardTypeResolver {
        return LegacyCardTypeResolver(
            cardProvider = Provider {
                requireNotNull(reduxStateHolder.userWalletsListManager?.selectedUserWalletSync?.scanResponse?.card)
            },
            productTypeProvider = Provider {
                requireNotNull(
                    value = reduxStateHolder.userWalletsListManager?.selectedUserWalletSync?.scanResponse?.productType,
                )
            },
            walletDataProvider = Provider {
                requireNotNull(reduxStateHolder.userWalletsListManager?.selectedUserWalletSync?.scanResponse).walletData
            },
        )
    }
}