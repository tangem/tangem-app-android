package com.tangem.feature.wallet.presentation.wallet.loaders

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.loaders.implementors.MultiWalletContentLoaderFactory
import com.tangem.feature.wallet.presentation.wallet.loaders.implementors.SingleWalletContentLoaderFactory
import com.tangem.feature.wallet.presentation.wallet.loaders.implementors.SingleWalletWithTokenContentLoaderFactory
import com.tangem.feature.wallet.presentation.wallet.loaders.implementors.WalletContentLoader
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
internal class WalletContentLoaderFactory @Inject constructor(
    private val multiWalletContentLoaderFactory: MultiWalletContentLoaderFactory,
    private val singleWalletWithTokenContentLoaderFactory: SingleWalletWithTokenContentLoaderFactory,
    private val singleWalletContentLoaderFactory: SingleWalletContentLoaderFactory,
) {

    fun create(
        userWallet: UserWallet,
        appCurrency: AppCurrency,
        clickIntents: WalletClickIntentsV2,
        isRefresh: Boolean = false,
    ): WalletContentLoader? {
        return when {
            userWallet.isMultiCurrency -> {
                multiWalletContentLoaderFactory.create(userWallet, appCurrency, clickIntents)
            }
            userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken() -> {
                singleWalletWithTokenContentLoaderFactory.create(userWallet, appCurrency, clickIntents)
            }
            !userWallet.isMultiCurrency -> {
                singleWalletContentLoaderFactory.create(userWallet, appCurrency, clickIntents, isRefresh)
            }
            else -> null
        }
    }
}