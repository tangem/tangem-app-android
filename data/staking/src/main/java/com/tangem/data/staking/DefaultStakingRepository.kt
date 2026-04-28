package com.tangem.data.staking

import arrow.core.getOrElse
import com.tangem.data.staking.store.StakeKitBalancesStore
import com.tangem.domain.card.common.TapWorkarounds.isWallet2
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.repositories.P2PEthPoolRepository
import com.tangem.domain.staking.repositories.StakeKitRepository
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.staking.toggles.StakingFeatureToggles
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.lib.crypto.BlockchainUtils.isCardano
import com.tangem.lib.crypto.BlockchainUtils.isSolana
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

import kotlinx.coroutines.withContext

internal class DefaultStakingRepository(
    private val stakeKitRepository: StakeKitRepository,
    private val p2pEthPoolRepository: P2PEthPoolRepository,
    private val stakeKitBalancesStore: StakeKitBalancesStore,
    private val dispatchers: CoroutineDispatcherProvider,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val stakingFeatureToggles: StakingFeatureToggles,
) : StakingRepository {

    override fun getStakingAvailability(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Flow<StakingAvailability> {
        return channelFlow {
            val stakingIntegration = StakingIntegrationID.create(currencyId = cryptoCurrency.id)
            if (stakingIntegration == null || !stakingFeatureToggles.isIntegrationEnabled(stakingIntegration)) {
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

            val availabilityFlow = when (stakingIntegration) {
                StakingIntegrationID.P2PEthPool -> p2pEthPoolRepository.getStakingAvailability()
                is StakingIntegrationID.StakeKit -> stakeKitRepository.getStakingAvailability(
                    stakingIntegration,
                    rawCurrencyId,
                    cryptoCurrency.symbol,
                )
            }

            availabilityFlow.collect { send(it) }
        }
    }

    override suspend fun getStakingAvailabilitySync(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): StakingAvailability {
        val stakingIntegration = StakingIntegrationID.create(currencyId = cryptoCurrency.id)
            ?.takeIf(stakingFeatureToggles::isIntegrationEnabled)
            ?: return StakingAvailability.Unavailable

        if (checkForInvalidCardBatch(userWalletId, cryptoCurrency)) {
            return StakingAvailability.Unavailable
        }

        val rawCurrencyId = cryptoCurrency.id.rawCurrencyId
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
            val balances = stakeKitBalancesStore.getAllSyncOrNull(userWalletId) ?: return@withContext false

            val hasDataStakingBalance by lazy {
                balances.any { stakingBalance ->
                    stakingBalance is StakingBalance.Data
                }
            }

            balances.isNotEmpty() && hasDataStakingBalance
        }
    }

    private fun checkForInvalidCardBatch(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean {
        val userWallet = getUserWalletUseCase(userWalletId).getOrElse {
            error("Failed to get user wallet")
        }

        if (userWallet !is UserWallet.Cold) {
            return false
        }

        if (userWallet.scanResponse.productType == ProductType.Note) {
            return true
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