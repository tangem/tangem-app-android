package com.tangem.tap.di.domain

import com.tangem.blockchain.common.TransactionSigner
import com.tangem.data.wallets.hot.TangemHotWalletSigner
import com.tangem.domain.card.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.walletconnect.CheckIsWalletConnectAvailableUseCase
import com.tangem.domain.walletconnect.WcTransactionSignerProvider
import com.tangem.domain.walletconnect.repository.WalletConnectRepository
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import com.tangem.domain.walletconnect.usecase.WcSessionsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object WalletConnectDomainModule {

    @Provides
    @Singleton
    fun providesCheckIsWalletConnectAvailableUseCase(
        walletConnectRepository: WalletConnectRepository,
    ): CheckIsWalletConnectAvailableUseCase {
        return CheckIsWalletConnectAvailableUseCase(walletConnectRepository = walletConnectRepository)
    }

    @Provides
    @Singleton
    fun providesWcSessionsUseCase(sessionsManager: WcSessionsManager): WcSessionsUseCase {
        return WcSessionsUseCase(sessionsManager)
    }

    @Provides
    @Singleton
    fun providesWcTransactionSignerProvider(
        cardSdkConfigRepository: CardSdkConfigRepository,
        tangemHotWalletSignerFactory: TangemHotWalletSigner.Factory,
    ): WcTransactionSignerProvider {
        return object : WcTransactionSignerProvider {
            override fun createSigner(wallet: UserWallet): TransactionSigner {
                return when (wallet) {
                    is UserWallet.Hot -> tangemHotWalletSignerFactory.create(wallet)
                    is UserWallet.Cold -> {
                        val card = wallet.scanResponse.card
                        val isCardNotBackedUp = card.backupStatus?.isActive != true && !card.isTangemTwins
                        cardSdkConfigRepository.getCommonSigner(
                            cardId = card.cardId.takeIf { isCardNotBackedUp },
                            twinKey = TwinKey.getOrNull(scanResponse = wallet.scanResponse),
                        )
                    }
                }
            }
        }
    }
}