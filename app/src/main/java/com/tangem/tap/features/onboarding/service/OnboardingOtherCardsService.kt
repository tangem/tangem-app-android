package com.tangem.tap.features.onboarding.service

import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.tap.common.extensions.withMainContext
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.features.wallet.redux.AddressData
import com.tangem.tap.persistence.UsedCardsPrefStorage

/**
[REDACTED_AUTHOR]
 */
class OnboardingOtherCardsService(
    override val fromScreen: AppScreen,
    override var scanResponse: ScanResponse,
    cardInfoStorage: UsedCardsPrefStorage,
    walletManagerFactory: WalletManagerFactory,
) : ProductOnboardingService(cardInfoStorage, walletManagerFactory) {

    override fun navigateToScreen(): AppScreen {
        return AppScreen.OnboardingOther
    }

    // Return any value and any state. The balance of the card is not important in this service
    override suspend fun updateBalance(): OnboardingWalletBalance {
        val customError = TapError.CustomError("Loading cancelled. Cause: wallet manager didn't created")
        withMainContext { loadedBalance = OnboardingWalletBalance.error(customError) }
        return loadedBalance
    }

    override fun getToUpUrl(): String? {
        throw UnsupportedOperationException()
    }

    override fun getAddressData(): AddressData? {
        throw UnsupportedOperationException()
    }
}