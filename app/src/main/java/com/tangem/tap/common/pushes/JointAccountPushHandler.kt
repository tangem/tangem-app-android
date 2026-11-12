package com.tangem.tap.common.pushes

import com.tangem.common.routing.deeplink.DeeplinkConst.ACCOUNT_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.WALLET_ID_KEY
import com.tangem.core.decompose.utils.ForegroundActivityProvider
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.notifications.models.NotificationType
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch
import javax.inject.Inject

class JointAccountPushHandler @Inject constructor(
    private val appCoroutineScope: AppCoroutineScope,
    private val foregroundActivityProvider: ForegroundActivityProvider,
    private val jointAccountFeatureToggles: JointAccountFeatureToggles,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val singleAccountListFetcher: SingleAccountListFetcher,
) {

    fun handle(queryParams: Map<String, String>) {
        if (NotificationType.getType(queryParams[TYPE_KEY]) != NotificationType.JointTokens) return
        if (!jointAccountFeatureToggles.isJointAccountEnabled) return
        if (foregroundActivityProvider.foregroundActivity == null) return
        appCoroutineScope.launch { refreshWalletIfJointAccount(queryParams) }
    }

    internal suspend fun refreshWalletIfJointAccount(queryParams: Map<String, String>) {
        val userWalletId = queryParams[WALLET_ID_KEY]?.let(UserWalletId::fromHexOrNull) ?: return
        val accountValue = queryParams[ACCOUNT_ID_KEY] ?: return
        val accountId = AccountId.forJointAccount(userWalletId = userWalletId, value = accountValue).getOrNull()
            ?: return

        val userWallet = getUserWalletUseCase(userWalletId).getOrNull() ?: return
        if (userWallet.isLocked || !userWallet.isMultiCurrency) return

        val isJointAccount = singleAccountListSupplier.getSyncOrNull(userWalletId)
            ?.accounts
            ?.any { it.accountId == accountId && it is Account.Joint } == true
        if (!isJointAccount) return

        singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId = userWalletId))
            .onLeft { TangemLogger.e("Error on refreshing joint account tokens from push", it) }
    }
}