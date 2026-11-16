package com.tangem.feature.wallet.presentation.wallet.analytics.utils

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.AccountType
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.NoticeShown
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.Placement
import com.tangem.domain.tangempay.utils.isKycAdditionalInfoActionRequired
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.utils.ScreenLifecycleProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@ModelScoped
internal class WalletTangemPayAnalyticsEventSender @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val screenLifecycleProvider: ScreenLifecycleProvider,
    private val stateController: WalletStateController,
) {

    private val homeRowNotices = mutableMapOf<HomeRowKey, HomeRowNotice>()
    private var selectedWalletId: UserWalletId? = null

    fun observeSelectedWallet(coroutineScope: CoroutineScope) {
        stateController.selectedWalletIdFlow
            .onEach(::onWalletSelected)
            .launchIn(coroutineScope)
    }

    @Synchronized
    fun send(status: AccountStatus.Payment) {
        val isRequired = when (val value = status.value) {
            PaymentAccountStatusValue.Loading,
            PaymentAccountStatusValue.Error.ExposedDevice,
            PaymentAccountStatusValue.Error.Unavailable,
            PaymentAccountStatusValue.Error.NotSynced,
            -> return
            is PaymentAccountStatusValue.Loaded -> value.isKycAdditionalInfoActionRequired
            else -> false
        }
        updateHomeRowNotice(
            key = HomeRowKey(userWalletId = status.account.userWalletId, accountType = AccountType.PAYMENT),
            isRequired = isRequired,
        )
    }

    @Synchronized
    fun send(status: AccountStatus.Money) {
        val isRequired = when (val value = status.value) {
            is MoneyAccountStatusValue.Error -> return
            is MoneyAccountStatusValue.Account.Active -> value.isKycAdditionalInfoRequired
            else -> false
        }
        updateHomeRowNotice(
            key = HomeRowKey(userWalletId = status.account.userWalletId, accountType = AccountType.MONEY),
            isRequired = isRequired,
        )
    }

    @Synchronized
    fun onAccountOpened(userWalletId: UserWalletId, accountType: AccountType) {
        val key = HomeRowKey(userWalletId = userWalletId, accountType = accountType)
        val notice = homeRowNotices[key] ?: return
        homeRowNotices[key] = notice.copy(isSent = false)
    }

    @Synchronized
    fun onResume() {
        sendSelectedWalletNotices()
    }

    @Synchronized
    private fun onWalletSelected(userWalletId: UserWalletId) {
        selectedWalletId = userWalletId
        homeRowNotices.replaceAll { key, notice ->
            if (key.userWalletId == userWalletId) notice.copy(isSent = false) else notice
        }

        if (!screenLifecycleProvider.isBackgroundState.value) {
            sendSelectedWalletNotices()
        }
    }

    private fun updateHomeRowNotice(key: HomeRowKey, isRequired: Boolean) {
        val isSent = isRequired && homeRowNotices[key]?.isSent == true
        homeRowNotices[key] = HomeRowNotice(isRequired = isRequired, isSent = isSent)

        if (key.userWalletId == selectedWalletId && !screenLifecycleProvider.isBackgroundState.value) {
            sendHomeRowNoticeIfPending(key)
        }
    }

    private fun sendSelectedWalletNotices() {
        homeRowNotices.keys
            .filter { key -> key.userWalletId == selectedWalletId }
            .forEach(::sendHomeRowNoticeIfPending)
    }

    private fun sendHomeRowNoticeIfPending(key: HomeRowKey) {
        val notice = homeRowNotices[key] ?: return
        if (!notice.isRequired || notice.isSent) return

        analyticsEventHandler.send(NoticeShown(accountType = key.accountType, placement = Placement.HOME_ROW))
        homeRowNotices[key] = notice.copy(isSent = true)
    }

    private data class HomeRowKey(val userWalletId: UserWalletId, val accountType: AccountType)

    private data class HomeRowNotice(val isRequired: Boolean, val isSent: Boolean)
}