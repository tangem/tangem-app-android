package com.tangem.tap.features.onboarding.service

import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.persistence.UsedCardsPrefStorage

/**
 * Created by Anton Zhilenkov on 05/10/2021.
 */
class OnboardingNoteService(
    override val fromScreen: AppScreen,
    override var scanResponse: ScanResponse,
    cardInfoStorage: UsedCardsPrefStorage,
    walletManagerFactory: WalletManagerFactory,
) : ProductOnboardingService(cardInfoStorage, walletManagerFactory) {

    override fun navigateToScreen(): AppScreen {
        return AppScreen.OnboardingNote
    }
}