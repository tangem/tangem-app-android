package com.tangem.feature.wallet.presentation.wallet.loaders

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.feature.wallet.presentation.wallet.loaders.implementors.*
import javax.inject.Inject

@ModelScoped
internal class WalletContentLoaderFactory @Inject constructor(
    private val multiWalletContentLoaderFactory: MultiWalletContentLoader.Factory,
    private val singleWalletContentLoader: SingleWalletContentLoader.Factory,
) {

    fun create(userWallet: UserWallet): WalletContentLoader? {
        return when {
            userWallet.isMultiCurrency -> {
                multiWalletContentLoaderFactory.create(userWallet)
            }
            userWallet is UserWallet.Cold -> {
                singleWalletContentLoader.create(userWallet)
            }
            else -> null
        }
    }
}