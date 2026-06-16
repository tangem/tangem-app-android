package com.tangem.data.account.producer

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.none
import com.tangem.common.card.FirmwareVersion
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.producer.SingleAccountListProducer
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.virtualaccount.VirtualAccountFeatureToggles
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Default implementation of [SingleAccountListProducer].
 * Produces a list of [AccountList] for a specific user wallet.
 *
 * @property params                       params containing the user wallet ID
 * @property walletAccountListFlowFactory builder to create flows of [AccountList] for each wallet
 * @property dispatchers                  coroutine dispatchers provider
 *
[REDACTED_AUTHOR]
 */
internal class DefaultSingleAccountListProducer @AssistedInject constructor(
    @Assisted val params: SingleAccountListProducer.Params,
    override val flowProducerTools: FlowProducerTools,
    private val walletAccountListFlowFactory: WalletAccountListFlowFactory,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val virtualAccountsFeatureToggles: VirtualAccountFeatureToggles,
    private val dispatchers: CoroutineDispatcherProvider,
) : SingleAccountListProducer {

    override val fallback: Option<AccountList> = none()

    private val logger = TangemLogger.withTag(TAG)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun produce(): Flow<AccountList> {
        val walletId = params.userWalletId
        logger.i("produce() called for $walletId")
        return walletAccountListFlowFactory.create(walletId)
            .map { accountList ->
                val userWallet = userWalletsListRepository.getSyncStrict(id = walletId)
                accountList
                    .addAccountIf(userWallet.isPaymentAccountSupported()) { Account.Payment(walletId) }
                    .addAccountIf(userWallet.isVirtualAccountSupported()) { Account.Virtual(walletId) }
            }
            .flowOn(dispatchers.default)
    }

    private fun UserWallet.isPaymentAccountSupported(): Boolean = when (this) {
        is UserWallet.Cold -> scanResponse.card.firmwareVersion >= FirmwareVersion.HDWalletAvailable
        is UserWallet.Hot -> hotWalletId.authType != HotWalletId.AuthType.NoPassword
    }

    private fun UserWallet.isVirtualAccountSupported(): Boolean {
        if (!virtualAccountsFeatureToggles.isVirtualAccountsEnabled) return false

        return when (this) {
            is UserWallet.Cold -> scanResponse.card.firmwareVersion >= FirmwareVersion.HDWalletAvailable
            is UserWallet.Hot -> hotWalletId.authType != HotWalletId.AuthType.NoPassword
        }
    }

    private inline fun AccountList.addAccountIf(condition: Boolean, account: () -> Account): AccountList {
        return if (condition) {
            plus(account()).getOrElse { throwable ->
                error("Can not combine account list and special account: $throwable")
            }
        } else {
            this
        }
    }

    @AssistedFactory
    interface Factory : SingleAccountListProducer.Factory {
        override fun create(params: SingleAccountListProducer.Params): DefaultSingleAccountListProducer
    }

    private companion object {
        const val TAG = "SingleAccountListProducer"
    }
}