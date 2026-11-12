package com.tangem.features.earn.flow.impl.model

import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.amountScreen.converters.AmountAccountConverter
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.earn.product.api.product.EarnProduct
import javax.inject.Inject

internal class EarnFlowWallet @Inject constructor(
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
) {

    fun userWallet(product: EarnProduct.Params): UserWallet? = getUserWalletUseCase(product.userWalletId).getOrNull()

    /** "From <account>" with several accounts, "From <wallet>" otherwise, as the Send amount screen names it. */
    suspend fun source(product: EarnProduct.Params, userWallet: UserWallet?): AccountTitleUM {
        val account = getAccountCurrencyStatusUseCase.invokeSync(product.userWalletId, product.currency)
            .getOrNull()
            ?.account
        val walletName = userWallet?.name.orEmpty()
        return AmountAccountConverter(
            prefixText = resourceReference(R.string.common_from),
            isAccountsMode = isAccountsModeEnabledUseCase.invokeSync(),
            walletTitle = resourceReference(R.string.send_from_wallet_name, wrappedList(walletName)),
        ).convert(account)
    }
}