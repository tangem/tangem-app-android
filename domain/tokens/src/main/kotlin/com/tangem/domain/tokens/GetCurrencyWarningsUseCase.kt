package com.tangem.domain.tokens

import com.tangem.blockchainsdk.utils.isNeedToCreateAccountWithoutReserve
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.CurrencyAmount
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.tokens.model.warnings.HederaWarnings
import com.tangem.domain.tokens.model.warnings.KaspaWarnings
import com.tangem.domain.tokens.operations.BaseCurrencyStatusOperations
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.transaction.models.AssetRequirementsCondition
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import java.math.BigDecimal

@Suppress("LongParameterList")
class GetCurrencyWarningsUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    private val currenciesRepository: CurrenciesRepository,
    private val dispatchers: CoroutineDispatcherProvider,
    private val currencyChecksRepository: CurrencyChecksRepository,
    private val currencyStatusOperations: BaseCurrencyStatusOperations,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currencyStatus: CryptoCurrencyStatus,
        derivationPath: Network.DerivationPath,
        isSingleWalletWithTokens: Boolean,
    ): Flow<Set<CryptoCurrencyWarning>> {
        val currency = currencyStatus.currency

        // don't add here notifications that require async requests
        return combine(
            getCoinRelatedWarnings(
                userWalletId = userWalletId,
                networkId = currency.network.id,
                currencyId = currency.id,
                derivationPath = derivationPath,
                isSingleWalletWithTokens = isSingleWalletWithTokens,
            ),
            flowOf(currencyChecksRepository.getRentInfoWarning(userWalletId, currencyStatus)),
            flowOf(currencyChecksRepository.getExistentialDeposit(userWalletId, currency.network)),
            flowOf(currencyChecksRepository.getFeeResourceAmount(userWalletId, currency.network)),
        ) { coinRelatedWarnings, maybeRentWarning, maybeEdWarning, maybeFeeResource ->
            setOfNotNull(
                maybeRentWarning,
                maybeEdWarning?.let { getExistentialDepositWarning(currency, it) },
                maybeFeeResource?.let { getFeeResourceWarning(it) },
                * coinRelatedWarnings.toTypedArray(),
                getNetworkUnavailableWarning(currencyStatus),
                getNetworkNoAccountWarning(currencyStatus),
                getBeaconChainShutdownWarning(rawId = currency.network.id.rawId),
                getAssetRequirementsWarning(userWalletId = userWalletId, currency = currency),
                getMigrationFromMaticToPolWarning(currency),
            )
        }.flowOn(dispatchers.io)
    }

    @Suppress("LongParameterList")
    private suspend fun getCoinRelatedWarnings(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        currencyId: CryptoCurrency.ID,
        derivationPath: Network.DerivationPath,
        isSingleWalletWithTokens: Boolean,
    ): Flow<List<CryptoCurrencyWarning>> {
        val currencyFlow = currencyStatusOperations.getCurrencyStatusFlow(
            userWalletId = userWalletId,
            currencyId = currencyId,
            isSingleWalletWithTokens = isSingleWalletWithTokens,
        )

        val networkFlow = if (isSingleWalletWithTokens) {
            currencyStatusOperations.getNetworkCoinForSingleWalletWithTokenFlow(userWalletId, networkId)
        } else {
            currencyStatusOperations.getNetworkCoinFlow(userWalletId, networkId, derivationPath)
        }

        return combine(
            currencyFlow.map { it.getOrNull() },
            networkFlow.map { it.getOrNull() },
        ) { tokenStatus, coinStatus ->
            when {
                tokenStatus != null && coinStatus != null -> {
                    buildList {
                        getUsedOutdatedDataWarning(tokenStatus)?.let(::add)
                        getFeeWarning(
                            userWalletId = userWalletId,
                            coinStatus = coinStatus,
                            tokenStatus = tokenStatus,
                        )?.let(::add)
                    }
                }
                else -> listOf(CryptoCurrencyWarning.SomeNetworksUnreachable)
            }
        }
    }

    private suspend fun getFeeWarning(
        userWalletId: UserWalletId,
        coinStatus: CryptoCurrencyStatus,
        tokenStatus: CryptoCurrencyStatus,
    ): CryptoCurrencyWarning? {
        val feePaidCurrency = currenciesRepository.getFeePaidCurrency(userWalletId, tokenStatus.currency.network)
        val isNetworkFeeZero = currenciesRepository.isNetworkFeeZero(userWalletId, tokenStatus.currency.network)
        return when {
            feePaidCurrency is FeePaidCurrency.Coin &&
                !tokenStatus.value.amount.isZero() &&
                coinStatus.value.amount.isZero() &&
                !isNetworkFeeZero -> {
                CryptoCurrencyWarning.BalanceNotEnoughForFee(
                    tokenCurrency = tokenStatus.currency,
                    coinCurrency = coinStatus.currency,
                )
            }
            feePaidCurrency is FeePaidCurrency.Token -> {
                val feePaidTokenBalance = feePaidCurrency.balance
                val amount = tokenStatus.value.amount ?: return null
                if (!amount.isZero() && feePaidTokenBalance.isZero() && !isNetworkFeeZero) {
                    constructTokenBalanceNotEnoughWarning(
                        userWalletId = userWalletId,
                        tokenStatus = tokenStatus,
                        feePaidToken = feePaidCurrency,
                    )
                } else {
                    null
                }
            }
            else -> null
        }
    }

    private fun getUsedOutdatedDataWarning(status: CryptoCurrencyStatus): CryptoCurrencyWarning? {
        return CryptoCurrencyWarning.UsedOutdatedDataWarning.takeIf {
            status.value.sources.total == StatusSource.ONLY_CACHE
        }
    }

    private suspend fun constructTokenBalanceNotEnoughWarning(
        userWalletId: UserWalletId,
        tokenStatus: CryptoCurrencyStatus,
        feePaidToken: FeePaidCurrency.Token,
    ): CryptoCurrencyWarning {
        val tokens = if (tokensFeatureToggles.isWalletBalanceFetcherEnabled) {
            multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
            )
                .orEmpty()
        } else {
            currenciesRepository.getMultiCurrencyWalletCurrenciesSync(userWalletId)
        }

        val token = tokens.find {
            it is CryptoCurrency.Token &&
                it.contractAddress.equals(feePaidToken.contractAddress, ignoreCase = true) &&
                it.network.derivationPath == tokenStatus.currency.network.derivationPath
        }

        return if (token != null) {
            CryptoCurrencyWarning.CustomTokenNotEnoughForFee(
                currency = tokenStatus.currency,
                feeCurrency = token,
                networkName = token.network.name,
                feeCurrencyName = feePaidToken.name,
                feeCurrencySymbol = feePaidToken.symbol,
            )
        } else {
            CryptoCurrencyWarning.CustomTokenNotEnoughForFee(
                currency = tokenStatus.currency,
                feeCurrency = null,
                networkName = tokenStatus.currency.network.name,
                feeCurrencyName = feePaidToken.name,
                feeCurrencySymbol = feePaidToken.symbol,
            )
        }
    }

    private fun getNetworkUnavailableWarning(currencyStatus: CryptoCurrencyStatus): CryptoCurrencyWarning? {
        return (currencyStatus.value as? CryptoCurrencyStatus.Unreachable)?.let {
            CryptoCurrencyWarning.SomeNetworksUnreachable
        }
    }

    private fun getNetworkNoAccountWarning(currencyStatus: CryptoCurrencyStatus): CryptoCurrencyWarning? {
        return (currencyStatus.value as? CryptoCurrencyStatus.NoAccount)?.let {
            if (isNeedToCreateAccountWithoutReserve(networkId = currencyStatus.currency.network.rawId)) {
                CryptoCurrencyWarning.TopUpWithoutReserve
            } else {
                CryptoCurrencyWarning.SomeNetworksNoAccount(
                    amountToCreateAccount = it.amountToCreateAccount,
                    amountCurrency = currencyStatus.currency,
                )
            }
        }
    }

    private fun getBeaconChainShutdownWarning(rawId: Network.RawID): CryptoCurrencyWarning.BeaconChainShutdown? {
        return if (BlockchainUtils.isBeaconChain(rawId.value)) CryptoCurrencyWarning.BeaconChainShutdown else null
    }

    private fun getExistentialDepositWarning(
        currency: CryptoCurrency,
        amount: BigDecimal,
    ): CryptoCurrencyWarning.ExistentialDeposit {
        return CryptoCurrencyWarning.ExistentialDeposit(
            currencyName = currency.name,
            edStringValueWithSymbol = "${amount.toPlainString()} ${currency.symbol}",
        )
    }

    private fun getFeeResourceWarning(feeResource: CurrencyAmount): CryptoCurrencyWarning.FeeResourceInfo {
        return CryptoCurrencyWarning.FeeResourceInfo(
            amount = feeResource.value,
            maxAmount = feeResource.maxValue,
        )
    }

    private suspend fun getAssetRequirementsWarning(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
    ): CryptoCurrencyWarning? {
        return when (val requirements = walletManagersFacade.getAssetRequirements(userWalletId, currency)) {
            is AssetRequirementsCondition.PaidTransaction -> HederaWarnings.AssociateWarning(currency = currency)
            is AssetRequirementsCondition.RequiredTrustline -> CryptoCurrencyWarning.RequiredTrustline(
                currency = currency,
                requiredAmount = requirements.requiredAmount,
                currencyDecimals = requirements.decimals,
                currencySymbol = requirements.currencySymbol,
            )
            is AssetRequirementsCondition.PaidTransactionWithFee -> {
                HederaWarnings.AssociateWarningWithFee(
                    currency = currency,
                    fee = requirements.feeAmount,
                    feeCurrencySymbol = requirements.feeCurrencySymbol,
                    feeCurrencyDecimals = requirements.decimals,
                )
            }
            is AssetRequirementsCondition.IncompleteTransaction ->
                KaspaWarnings.IncompleteTransaction(
                    currency = currency,
                    amount = requirements.amount,
                    currencySymbol = requirements.currencySymbol,
                    currencyDecimals = requirements.currencyDecimals,
                )

            null -> null
        }
    }

    private fun getMigrationFromMaticToPolWarning(currency: CryptoCurrency): CryptoCurrencyWarning? {
        return if (currency.symbol == MATIC_SYMBOL && !BlockchainUtils.isPolygonChain(currency.network.rawId)) {
            CryptoCurrencyWarning.MigrationMaticToPol
        } else {
            null
        }
    }

    private fun BigDecimal?.isZero(): Boolean {
        return this?.signum() == 0
    }

    companion object {
        private const val MATIC_SYMBOL = "MATIC"
    }
}