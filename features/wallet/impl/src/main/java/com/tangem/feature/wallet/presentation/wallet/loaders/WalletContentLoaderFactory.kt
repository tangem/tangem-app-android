package com.tangem.feature.wallet.presentation.wallet.loaders

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.feature.wallet.presentation.wallet.loaders.implementors.MultiWalletContentLoader
import com.tangem.feature.wallet.presentation.wallet.loaders.implementors.SingleWalletContentLoader
import com.tangem.feature.wallet.presentation.wallet.loaders.implementors.SingleWalletWithTokenContentLoader
import com.tangem.feature.wallet.presentation.wallet.loaders.implementors.WalletContentLoader
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class WalletContentLoaderFactory @Inject constructor(
    private val multiWalletContentLoaderFactory: MultiWalletContentLoader.Factory,
    private val singleWalletWithTokenContentLoaderFactory: SingleWalletWithTokenContentLoader.Factory,
    private val singleWalletContentLoaderFactory: SingleWalletContentLoader.Factory,
) {

    fun create(userWallet: UserWallet, isRefresh: Boolean = false): WalletContentLoader? {
        return when {
            userWallet.isMultiCurrency -> {
                multiWalletContentLoaderFactory.create(userWallet)
            }
            userWallet is UserWallet.Cold && userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken() -> {
                singleWalletWithTokenContentLoaderFactory.create(userWallet)
            }
            userWallet is UserWallet.Cold && !userWallet.isMultiCurrency -> {
                singleWalletContentLoaderFactory.create(userWallet, isRefresh)
            }
            else -> null
        }
    }
}