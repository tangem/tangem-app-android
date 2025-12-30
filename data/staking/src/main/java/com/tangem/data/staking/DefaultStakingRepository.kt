package com.tangem.data.staking

import arrow.core.getOrElse
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.data.staking.store.StakingBalancesStore
import com.tangem.domain.card.common.TapWorkarounds.isWallet2
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.repositories.P2PEthPoolRepository
import com.tangem.domain.staking.repositories.StakeKitRepository
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.staking.toggles.StakingFeatureToggles
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.lib.crypto.BlockchainUtils.isCardano
import com.tangem.lib.crypto.BlockchainUtils.isSolana
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
internal class DefaultStakingRepository(
    private val stakeKitRepository: StakeKitRepository,
    private val p2pEthPoolRepository: P2PEthPoolRepository,
    private val stakingBalanceStoreV2: StakingBalancesStore,
    private val dispatchers: CoroutineDispatcherProvider,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val stakingFeatureToggles: StakingFeatureToggles,
    private val walletManagersFacade: WalletManagersFacade,
) : StakingRepository {

    override fun getStakingAvailability(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Flow<StakingAvailability> {
        return channelFlow {
            if (!checkFeatureToggleEnabled(userWalletId, cryptoCurrency)) {
                send(StakingAvailability.Unavailable)
                return@channelFlow
            }

            if (checkForInvalidCardBatch(userWalletId, cryptoCurrency)) {
                send(StakingAvailability.Unavailable)
                return@channelFlow
            }

            val rawCurrencyId = cryptoCurrency.id.rawCurrencyId
            if (rawCurrencyId == null) {
                send(StakingAvailability.Unavailable)
                return@channelFlow
            }

            val stakingIntegration = StakingIntegrationID.create(currencyId = cryptoCurrency.id)

            val availabilityFlow = when (stakingIntegration) {
                StakingIntegrationID.P2PEthPool -> p2pEthPoolRepository.getStakingAvailability()
                is StakingIntegrationID.StakeKit -> stakeKitRepository.getStakingAvailability(
                    stakingIntegration,
                    rawCurrencyId,
                    cryptoCurrency.symbol,
                )
                null -> flowOf(StakingAvailability.Unavailable)
            }

            availabilityFlow.collect { send(it) }
        }
    }

    override suspend fun getStakingAvailabilitySync(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): StakingAvailability {
        if (!checkFeatureToggleEnabled(userWalletId, cryptoCurrency)) {
            return StakingAvailability.Unavailable
        }

        if (checkForInvalidCardBatch(userWalletId, cryptoCurrency)) {
            return StakingAvailability.Unavailable
        }

        val rawCurrencyId = cryptoCurrency.id.rawCurrencyId
        if (rawCurrencyId == null) {
            return StakingAvailability.Unavailable
        }

        val stakingIntegration = StakingIntegrationID.create(currencyId = cryptoCurrency.id)
            ?: return StakingAvailability.Unavailable

        return when (stakingIntegration) {
            StakingIntegrationID.P2PEthPool -> p2pEthPoolRepository.getStakingAvailabilitySync()
            is StakingIntegrationID.StakeKit -> stakeKitRepository.getStakingAvailabilitySync(
                stakingIntegration,
                rawCurrencyId,
                cryptoCurrency.symbol,
            )
        }
    }

    override suspend fun isAnyTokenStaked(userWalletId: UserWalletId): Boolean {
        return withContext(dispatchers.default) {
            val balances = stakingBalanceStoreV2.getAllSyncOrNull(userWalletId) ?: return@withContext false

            val hasDataStakingBalance by lazy {
                balances.any { stakingBalance ->
                    stakingBalance is StakingBalance.Data
                }
            }

            balances.isNotEmpty() && hasDataStakingBalance
        }
    }

    private suspend fun checkFeatureToggleEnabled(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean {
        return when (cryptoCurrency.network.id.toBlockchain()) {
            Blockchain.TON -> stakingFeatureToggles.isTonStakingEnabled
            Blockchain.Ethereum -> {
                when (cryptoCurrency) {
                    is CryptoCurrency.Coin -> stakingFeatureToggles.isEthStakingEnabled
                    is CryptoCurrency.Token -> true
                }
            }
            Blockchain.Cardano -> {
                val address = walletManagersFacade.getDefaultAddress(userWalletId, cryptoCurrency.network).orEmpty()
                val balance = stakingBalanceStoreV2.getSyncOrNull(
                    userWalletId = userWalletId,
                    stakingId = StakingID(
                        integrationId = StakingIntegrationID.create(currencyId = cryptoCurrency.id)?.value
                            ?: return false,
                        address = address,
                    ),
                )
                if ((balance as? StakingBalance.Data.StakeKit)?.balance?.items?.isNotEmpty() == true) {
                    return true
                } else {
                    stakingFeatureToggles.isCardanoStakingEnabled
                }
            }
            else -> true
        }
    }

    private fun checkForInvalidCardBatch(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean {
        val userWallet = getUserWalletUseCase(userWalletId).getOrElse {
            error("Failed to get user wallet")
        }

        if (userWallet !is UserWallet.Cold) {
            return false
        }

        val blockchainId = cryptoCurrency.network.rawId
        return when {
            isSolana(blockchainId) -> INVALID_BATCHES_FOR_SOLANA.contains(userWallet.scanResponse.card.batchId)
            isCardano(blockchainId) -> !userWallet.scanResponse.card.isWallet2
            else -> false
        }
    }

    companion object {
        private val INVALID_BATCHES_FOR_SOLANA = listOf("AC01", "CB79")
    }
}