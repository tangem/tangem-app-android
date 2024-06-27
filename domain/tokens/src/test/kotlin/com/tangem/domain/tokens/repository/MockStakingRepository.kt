package com.tangem.domain.tokens.repository

import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.core.lce.lceFlow
import com.tangem.domain.staking.model.*
import com.tangem.domain.staking.model.action.EnterAction
import com.tangem.domain.staking.model.action.StakingActionStatus
import com.tangem.domain.staking.model.action.StakingActionType
import com.tangem.domain.staking.model.transaction.StakingTransaction
import com.tangem.domain.staking.model.transaction.StakingTransactionStatus
import com.tangem.domain.staking.model.transaction.StakingTransactionType
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyAddress
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import org.joda.time.DateTime
import java.math.BigDecimal

class MockStakingRepository : StakingRepository {
    override fun isStakingSupported(currencyId: String): Boolean = true

    override suspend fun fetchEnabledYields(refresh: Boolean) { /* no-op */
    }

    override suspend fun getEntryInfo(integrationId: String): StakingEntryInfo = StakingEntryInfo(
        interestRate = 1.toBigDecimal(),
        periodInDays = 2,
        tokenSymbol = "SOL",
    )

    override suspend fun getYield(cryptoCurrencyId: CryptoCurrency.ID, symbol: String): Yield = Yield(
        id = "1",
        token = Token(
            name = "Solana",
            network = NetworkType.SOLANA,
            symbol = "SOL",
            decimals = 18,
            address = null,
            coinGeckoId = "solana",
            logoURI = null,
            isPoints = null,
        ),
        tokens = listOf(),
        args = Yield.Args(
            enter = Yield.Args.Enter(
                addresses = Yield.Args.Enter.Addresses(
                    address = AddressArgument(
                        required = false,
                        network = null,
                        minimum = null,
                        maximum = null,
                    ),
                    additionalAddresses = mapOf(),
                ),
                args = mapOf(),
            ),
            exit = null,
        ),
        status = Yield.Status(enter = false, exit = null),
        apy = 1.toBigDecimal(),
        rewardRate = 2.3,
        rewardType = Yield.RewardType.APR,
        metadata = Yield.Metadata(
            name = "Yield",
            logoUri = "",
            description = "",
            documentation = null,
            gasFeeToken = Token(
                name = "Solana",
                network = NetworkType.SOLANA,
                symbol = "SOL",
                decimals = 18,
                address = null,
                coinGeckoId = null,
                logoURI = null,
                isPoints = null,
            ),
            token = Token(
                name = "Solana",
                network = NetworkType.SOLANA,
                symbol = "SOL",
                decimals = 18,
                address = null,
                coinGeckoId = null,
                logoURI = null,
                isPoints = null,
            ),
            tokens = listOf(),
            type = "auto",
            rewardSchedule = "1",
            cooldownPeriod = Yield.Metadata.Period(days = 1),
            warmupPeriod = Yield.Metadata.Period(days = 1),
            rewardClaiming = "1",
            defaultValidator = null,
            minimumStake = null,
            supportsMultipleValidators = false,
            revshare = Yield.Metadata.Enabled(enabled = false),
            fee = Yield.Metadata.Enabled(enabled = false),
        ),
        validators = listOf(),
        isAvailable = false,
    )

    override suspend fun getStakingAvailabilityForActions(
        cryptoCurrencyId: CryptoCurrency.ID,
        symbol: String,
    ): StakingAvailability = StakingAvailability.Unavailable

    override suspend fun fetchSingleYieldBalance(
        userWalletId: UserWalletId,
        address: CryptoCurrencyAddress,
        refresh: Boolean,
    ) {
        /* no-op */
    }

    override fun getSingleYieldBalanceFlow(
        userWalletId: UserWalletId,
        address: CryptoCurrencyAddress,
    ): Flow<YieldBalance> = channelFlow {
        send(YieldBalance.Error)
    }

    override suspend fun getSingleYieldBalanceSync(
        userWalletId: UserWalletId,
        address: CryptoCurrencyAddress,
    ): YieldBalance = YieldBalance.Error

    override suspend fun fetchMultiYieldBalance(
        userWalletId: UserWalletId,
        addresses: List<CryptoCurrencyAddress>,
        refresh: Boolean,
    ) {
        /* no-op */
    }

    override fun getMultiYieldBalanceFlow(
        userWalletId: UserWalletId,
        addresses: List<CryptoCurrencyAddress>,
    ): Flow<YieldBalanceList> = channelFlow {
        send(
            YieldBalanceList.Data(
                balances = listOf(YieldBalance.Error),
            ),
        )
    }

    override fun getMultiYieldBalanceLce(
        userWalletId: UserWalletId,
        addresses: List<CryptoCurrencyAddress>,
    ): LceFlow<Throwable, YieldBalanceList> = lceFlow {
        send(
            YieldBalanceList.Data(
                balances = listOf(YieldBalance.Error),
            ),
        )
    }

    override suspend fun getMultiYieldBalanceSync(
        userWalletId: UserWalletId,
        addresses: List<CryptoCurrencyAddress>,
    ): YieldBalanceList = YieldBalanceList.Data(
        balances = listOf(YieldBalance.Error),
    )

    override suspend fun createEnterAction(
        integrationId: String,
        amount: BigDecimal,
        address: String,
        validatorAddress: String,
        token: Token,
    ): EnterAction = EnterAction(
        id = "quis",
        integrationId = "persequeris",
        status = StakingActionStatus.PROCESSING,
        type = StakingActionType.CLAIM_REWARDS,
        currentStepIndex = 8701,
        amount = BigDecimal.ZERO,
        validatorAddress = null,
        validatorAddresses = listOf(),
        transactions = listOf(),
        createdAt = DateTime.now()
    )

    override suspend fun constructTransaction(transactionId: String): StakingTransaction = StakingTransaction(
        id = "id",
        network = NetworkType.SOLANA,
        status = StakingTransactionStatus.SIGNED,
        type = StakingTransactionType.FREEZE_ENERGY,
        hash = null,
        signedTransaction = null,
        unsignedTransaction = null,
        stepIndex = 9368,
        error = null,
        gasEstimate = null,
        stakeId = null,
        explorerUrl = null,
        ledgerHwAppId = null,
        isMessage = false

    )
}
