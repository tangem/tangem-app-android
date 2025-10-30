package com.tangem.feature.wallet.presentation.wallet.loaders

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.loaders.implementors.*
import javax.inject.Inject

@ModelScoped
internal class WalletContentLoaderFactory @Inject constructor(
    private val multiWalletContentLoaderFactory: MultiWalletContentLoaderFactory,
    private val singleWalletWithTokenContentLoaderFactory: SingleWalletWithTokenContentLoaderFactory,
    private val accountsFeatureToggles: AccountsFeatureToggles,
    private val singleWalletContentLoaderFactory: SingleWalletContentLoaderFactory,
    private val singleWalletContentLoaderV2Factory: SingleWalletContentLoaderV2.Factory,
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
            userWallet is UserWallet.Cold && userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken() -> {
                singleWalletWithTokenContentLoaderFactory.create(userWallet, clickIntents)
            }
            userWallet is UserWallet.Cold && userWallet.scanResponse.cardTypesResolver.isVisaWallet() -> {
                visaWalletContentLoaderFactory.create(userWallet, clickIntents, isRefresh)
            }
            userWallet is UserWallet.Cold && !userWallet.isMultiCurrency -> {
                if (accountsFeatureToggles.isFeatureEnabled) {
                    singleWalletContentLoaderV2Factory.create(userWallet, isRefresh)
                } else {
                    singleWalletContentLoaderFactory.create(userWallet, clickIntents, isRefresh)
                }
            }
            else -> null
        }
    }
}