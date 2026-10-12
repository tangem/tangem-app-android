package com.tangem.data.account.producer

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.none
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.producer.SingleAccountListProducer
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.isAccountCreated
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isTangemPayCompatible
import com.tangem.domain.polymarket.PolymarketOnboardedStore
import com.tangem.domain.polymarket.isPredictionAccountSupported
import com.tangem.features.collectibles.api.CollectiblesFeatureToggles
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.features.moneyaccount.MoneyAccountFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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
@Suppress("LongParameterList")
internal class DefaultSingleAccountListProducer @AssistedInject constructor(
    @Assisted val params: SingleAccountListProducer.Params,
    override val flowProducerTools: FlowProducerTools,
    private val walletAccountListFlowFactory: WalletAccountListFlowFactory,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val moneyAccountsFeatureToggles: MoneyAccountFeatureToggles,
    private val polymarketFeatureToggles: PolymarketFeatureToggles,
    private val polymarketOnboardedStore: PolymarketOnboardedStore,
    private val collectiblesFeatureToggles: CollectiblesFeatureToggles,
    private val collectiblesAccountRepository: CollectiblesAccountRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) : SingleAccountListProducer {

    override val fallback: Option<AccountList> = none()

    private val logger = TangemLogger.withTag(TAG)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun produce(): Flow<AccountList> {
        val walletId = params.userWalletId
        logger.i("produce() called for $walletId")
        return combine(
            walletAccountListFlowFactory.create(walletId),
            predictionOnboardedFlow(walletId),
            collectiblesAccountCreatedFlow(walletId),
        ) { accountList, isPredictionOnboarded, isCollectiblesAccountCreated ->
            val userWallet = userWalletsListRepository.getSyncStrict(id = walletId)
            accountList
                .addAccountIf(userWallet.isTangemPayCompatible) { Account.Payment(walletId) }
                .addAccountIf(userWallet.isMoneyAccountSupported()) { Account.Money(walletId) }
                .addAccountIf(userWallet.isPredictionSupported(isPredictionOnboarded)) {
                    Account.Prediction(walletId)
                }
                .addAccountIf(isCollectiblesAccountCreated) { Account.Collectibles(walletId) }
        }
            .flowOn(dispatchers.default)
    }

    private fun predictionOnboardedFlow(walletId: UserWalletId): Flow<Boolean> {
        if (!polymarketFeatureToggles.isPolymarketEnabled) return flowOf(false)

        return polymarketOnboardedStore.isOnboarded(walletId)
    }

    private fun collectiblesAccountCreatedFlow(walletId: UserWalletId): Flow<Boolean> {
        if (!collectiblesFeatureToggles.isCollectiblesEnabled) return flowOf(false)

        return collectiblesAccountRepository.getStatus(walletId).map { status -> status.isAccountCreated }
    }

    private fun UserWallet.isMoneyAccountSupported(): Boolean {
        if (!moneyAccountsFeatureToggles.isMoneyAccountsEnabled) return false

        return isTangemPayCompatible
    }

    private fun UserWallet.isPredictionSupported(isOnboarded: Boolean): Boolean {
        return isPredictionAccountSupported && isOnboarded
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