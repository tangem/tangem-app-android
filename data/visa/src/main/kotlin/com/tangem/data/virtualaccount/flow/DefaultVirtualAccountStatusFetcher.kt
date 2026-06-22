package com.tangem.data.virtualaccount.flow

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.virtualaccount.store.VirtualAccountStatusesStore
import com.tangem.domain.card.common.visa.VisaUtilities
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.VirtualAccountStatusValue
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.virtualaccount.flow.VirtualAccountStatusFetcher
import com.tangem.domain.wallets.extension.hasDerivation
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList")
internal class DefaultVirtualAccountStatusFetcher @Inject constructor(
    private val virtualAccountStatusesStore: VirtualAccountStatusesStore,
    private val dispatchers: CoroutineDispatcherProvider,
    private val networkFactory: NetworkFactory,
    private val tangemPayCurrencyFactory: TangemPayCurrencyFactory,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
    private val singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
) : VirtualAccountStatusFetcher {

    override suspend fun invoke(params: VirtualAccountStatusFetcher.Params) = Either.catchOn(dispatchers.default) {
        val account = Account.Virtual(userWalletId = params.userWalletId)
        // TODO([REDACTED_TASK_KEY]): Replace with the real VA status fetch (provisioning state, balance and banking
        //  details) from the backend once Virtual Account status endpoints are available. Until then the
        //  account is surfaced as NotCreated so the entity flows through the app end-to-end.
        getBalance(params.userWalletId)
        virtualAccountStatusesStore.store(
            userWalletId = params.userWalletId,
            status = AccountStatus.Virtual(account = account, value = VirtualAccountStatusValue.NotCreated),
        )
    }.onLeft {
        virtualAccountStatusesStore.updateStatusSource(
            userWalletId = params.userWalletId,
            source = StatusSource.ONLY_CACHE,
        )
    }

    private suspend fun getBalance(userWalletId: UserWalletId): Either<VirtualAccountStatusValue, BigDecimal> {
        val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)

        val hasVirtualAccountDerivation = userWallet.hasDerivation(
            blockchain = VisaUtilities.visaBlockchain,
            derivationPath = VisaUtilities.virtualAccountDerivationPath.rawPath,
        )
        if (!hasVirtualAccountDerivation) {
            // TODO: Doston(VA) Derive will be implemented in  [REDACTED_TASK_KEY]
            TangemLogger.withTag(TAG).d("Virtual account is not derived")
            return VirtualAccountStatusValue.Error.NotSynced.left()
        }

        val network = networkFactory.create(
            blockchain = VisaUtilities.visaBlockchain,
            derivationPath =
            Network.DerivationPath.Custom(VisaUtilities.virtualAccountDerivationPath.rawPath),
            userWallet = userWallet,
        )
        if (network == null) {
            TangemLogger.withTag(TAG).d("Can not create network for Virtual account")
            return VirtualAccountStatusValue.Error.Unavailable.left()
        }
        val token = tangemPayCurrencyFactory.createVirtualAccountToken(userWalletId)

        singleNetworkStatusFetcher(
            SingleNetworkStatusFetcher.Params(
                userWalletId = userWalletId,
                network = network,
                extraTokens = setOf(token),
            ),
        )

        val verifiedStatus = singleNetworkStatusSupplier
            .getSyncOrNull(SingleNetworkStatusProducer.Params(userWalletId, network))
            ?.value as? NetworkStatus.Verified
        val balance = (verifiedStatus?.amounts?.get(token.id) as? NetworkStatus.Amount.Loaded)?.value

        return if (balance != null) {
            TangemLogger.withTag(TAG).d("VA on-chain balance = $balance")
            balance.right()
        } else {
            TangemLogger.withTag(TAG).d("Can not get VA balance")
            VirtualAccountStatusValue.Error.Unavailable.left()
        }
    }

    private companion object {
        private const val TAG = "VirtualAccountStatusFetcher"
    }
}