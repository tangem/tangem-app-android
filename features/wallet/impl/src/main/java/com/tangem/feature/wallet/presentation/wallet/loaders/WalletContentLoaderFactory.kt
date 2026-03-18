package com.tangem.feature.wallet.presentation.wallet.loaders

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.ui.DesignFeatureToggles
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.feature.wallet.presentation.wallet.loaders.implementors.*
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class WalletContentLoaderFactory @Inject constructor(
    private val multiWalletContentLoaderFactory: MultiWalletContentLoader.Factory,
    private val singleWalletWithTokenContentLoaderFactory: SingleWalletWithTokenContentLoader.Factory,
    private val singleWalletContentLoaderLegacyFactory: SingleWalletContentLoaderLegacy.Factory,
    private val singleWalletContentLoader: SingleWalletContentLoader.Factory,
    private val designFeatureToggles: DesignFeatureToggles,
) {

    fun create(userWallet: UserWallet, isRefresh: Boolean = false): WalletContentLoader? {
        return when {
            userWallet.isMultiCurrency -> {
                multiWalletContentLoaderFactory.create(userWallet)
            }
            userWallet is UserWallet.Cold && userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken() -> {
                if (designFeatureToggles.isRedesignEnabled) {
                    singleWalletContentLoader.create(userWallet)
                } else {
                    singleWalletWithTokenContentLoaderFactory.create(userWallet)
                }
            }
            userWallet is UserWallet.Cold && !userWallet.isMultiCurrency -> {
                if (designFeatureToggles.isRedesignEnabled) {
                    singleWalletContentLoader.create(userWallet)
                } else {
                    singleWalletContentLoaderLegacyFactory.create(userWallet, isRefresh)
                }
            }
            else -> null
        }
    }
}