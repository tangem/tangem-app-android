package com.tangem.feature.wallet.presentation.wallet.loaders

import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.loaders.implementors.*
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
internal class WalletContentLoaderFactory @Inject constructor(
    private val multiWalletContentLoaderFactory: MultiWalletContentLoaderFactory,
    private val singleWalletWithTokenContentLoaderFactory: SingleWalletWithTokenContentLoaderFactory,
    private val singleWalletContentLoaderFactory: SingleWalletContentLoaderFactory,
    private val visaWalletContentLoaderFactory: VisaWalletContentLoaderFactory,
) {

    fun create(
        userWallet: UserWallet,
        clickIntents: WalletClickIntents,
        isRefresh: Boolean = false,
    ): WalletContentLoader? {
        return when {
            userWallet.isMultiCurrency -> {
                multiWalletContentLoaderFactory.create(userWallet, clickIntents)
            }
            userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken() -> {
                singleWalletWithTokenContentLoaderFactory.create(userWallet, clickIntents)
            }
            userWallet.scanResponse.cardTypesResolver.isVisaWallet() -> {
                visaWalletContentLoaderFactory.create(userWallet, clickIntents, isRefresh)
            }
            !userWallet.isMultiCurrency -> {
                singleWalletContentLoaderFactory.create(userWallet, clickIntents, isRefresh)
            }
            else -> null
        }
    }
}