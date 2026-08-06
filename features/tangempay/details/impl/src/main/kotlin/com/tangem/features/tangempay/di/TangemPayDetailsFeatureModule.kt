package com.tangem.features.tangempay.di

import com.tangem.features.tangempay.account.DefaultTangemPayDetailsContainerComponent
import com.tangem.features.tangempay.card.details.CardDetailsEventListener
import com.tangem.features.tangempay.card.details.DefaultCardDetailsEventListener
import com.tangem.features.tangempay.cashback.api.TangemPayCashbackComponent
import com.tangem.features.tangempay.cashback.impl.DefaultTangemPayCashbackComponent
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent
import com.tangem.features.tangempay.components.TangemPayTransactionBottomSheetComponent
import com.tangem.features.tangempay.orderCard.api.TangemPayOrderCardComponent
import com.tangem.features.tangempay.orderCard.impl.DefaultTangemPayOrderCardComponent
import com.tangem.features.tangempay.txhistory.details.TangemPayTxHistoryDetailsComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TangemPayDetailsFeatureModule {

    @Binds
    @Singleton
    fun bindTangemPayDetailsContainerComponentFactory(
        factory: DefaultTangemPayDetailsContainerComponent.Factory,
    ): TangemPayDetailsContainerComponent.Factory

    @Binds
    @Singleton
    fun bindCardDetailsEventListener(impl: DefaultCardDetailsEventListener): CardDetailsEventListener

    @Binds
    @Singleton
    fun bindTangemPayTransactionBottomSheetComponentFactory(
        factory: TangemPayTxHistoryDetailsComponent.Factory,
    ): TangemPayTransactionBottomSheetComponent.Factory

    @Binds
    @Singleton
    fun bindTangemPayCashbackComponentFactory(
        factory: DefaultTangemPayCashbackComponent.Factory,
    ): TangemPayCashbackComponent.Factory

    @Binds
    @Singleton
    fun bindTangemPayOrderCardComponentFactory(
        factory: DefaultTangemPayOrderCardComponent.Factory,
    ): TangemPayOrderCardComponent.Factory
}