package com.tangem.features.jointaccount.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.DeepLinkRoute
import com.tangem.common.routing.deeplink.DeeplinkConst.ACCOUNT_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.ENTRY_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.WALLET_ID_KEY
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.account.status.utils.MainExpandedAccountsHolder
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.notifications.models.NotificationType
import com.tangem.domain.wallets.usecase.ResolveAndSelectUserWalletUseCase
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import com.tangem.features.pushnotifications.api.analytics.PushNotificationAnalyticEvents
import com.tangem.features.wallet.deeplink.WalletDeepLinkActionTrigger
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

@Suppress("LongParameterList")
internal class DefaultJointAccountDeepLinkHandler @AssistedInject constructor(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val queryParams: Map<String, String>,
    @Assisted private val isFromOnNewIntent: Boolean,
    private val appRouter: AppRouter,
    private val resolveAndSelectUserWalletUseCase: ResolveAndSelectUserWalletUseCase,
    private val singleAccountSupplier: SingleAccountSupplier,
    private val singleAccountListFetcher: SingleAccountListFetcher,
    private val jointAccountListFetcher: SingleJointAccountListFetcher,
    private val expandedAccountsHolder: MainExpandedAccountsHolder,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val walletDeepLinkActionTrigger: WalletDeepLinkActionTrigger,
    private val jointAccountFeatureToggles: JointAccountFeatureToggles,
) : JointAccountDeepLinkHandler {

    init {
        handleDeepLink()
    }

    private fun handleDeepLink() {
        if (!jointAccountFeatureToggles.isJointAccountEnabled) {
            TangemLogger.e("Joint account feature is disabled, ignoring deeplink")
            appRouter.popTo(AppRoute.Wallet)
            return
        }

        val walletId = queryParams[WALLET_ID_KEY]
        val accountId = queryParams[ACCOUNT_ID_KEY]
        val entry = queryParams[ENTRY_KEY]
        val type = NotificationType.getType(queryParams[TYPE_KEY])

        scope.launch {
            val userWalletId = walletId?.let(::UserWalletId)
            val userWallet = resolveAndSelectUserWalletUseCase(userWalletId) ?: return@launch

            val account = findJointAccount(userWallet, accountId)
            if (account == null) {
                TangemLogger.e("Could not find joint account for $ACCOUNT_ID_KEY: $accountId")
                appRouter.popTo(AppRoute.Wallet)
                return@launch
            }

            when (entry) {
                DeepLinkRoute.JointAccount.ENTRY_MEMBERS -> {
                    analyticsEventHandler.send(PushNotificationAnalyticEvents.NotificationOpened(type.name))
                    appRouter.push(
                        route = AppRoute.JointAccountMembers(
                            userWalletId = userWallet.walletId,
                            cryptoAccountId = account.accountId.value,
                        ),
                        onComplete = { walletDeepLinkActionTrigger.selectWallet(userWallet.walletId) },
                    )
                }
                DeepLinkRoute.JointAccount.ENTRY_OVERVIEW -> {
                    analyticsEventHandler.send(PushNotificationAnalyticEvents.NotificationOpened(type.name))
                    appRouter.popTo(AppRoute.Wallet) { isSuccess ->
                        if (isSuccess) {
                            expandedAccountsHolder.expandAccount(account.accountId)
                            walletDeepLinkActionTrigger.selectWallet(userWallet.walletId)
                        }
                    }
                }
                else -> TangemLogger.e("Unknown $ENTRY_KEY: $entry")
            }
        }
    }

    /**


     * On a warm tap ([isFromOnNewIntent]) force-refresh and, on a cache miss, wait for the refreshed list —
     * on cold start the regular auth flow already loads a fresh list, so no extra fetch is needed.
     */
    private suspend fun findJointAccount(userWallet: UserWallet, accountId: String?): Account.Joint? {
        if (accountId == null) return null
        val id = AccountId.forJointAccount(userWalletId = userWallet.walletId, value = accountId).getOrNull()
            ?: return null

        if (isFromOnNewIntent) {
            supervisorScope {
                launch {
                    singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId = userWallet.walletId))
                        .onLeft { TangemLogger.e("Error on refreshing wallet accounts", it) }
                }
                launch {
                    jointAccountListFetcher(
                        params = SingleJointAccountListFetcher.Params(userWalletId = userWallet.walletId),
                    ).onLeft { TangemLogger.e("Error on refreshing joint accounts", it) }
                }
            }
        }

        val accountFlow = singleAccountSupplier.filterJointAccount(id)
        return withTimeoutOrNull(ACCOUNT_APPEARANCE_TIMEOUT_MILLIS.milliseconds) { accountFlow.firstOrNull() }
    }

    @AssistedFactory
    interface Factory : JointAccountDeepLinkHandler.Factory {
        override fun create(
            coroutineScope: CoroutineScope,
            queryParams: Map<String, String>,
            isFromOnNewIntent: Boolean,
        ): DefaultJointAccountDeepLinkHandler
    }

    private companion object {
        const val ACCOUNT_APPEARANCE_TIMEOUT_MILLIS = 3_000L
    }
}