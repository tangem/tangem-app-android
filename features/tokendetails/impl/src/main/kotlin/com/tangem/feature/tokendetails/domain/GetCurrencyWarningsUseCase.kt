package com.tangem.feature.tokendetails.domain

import com.tangem.blockchainsdk.utils.isNeedToCreateAccountWithoutReserve
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCoinStatus
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCryptoCurrencyStatus
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.model.CurrencyAmount
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.tokens.model.warnings.HederaWarnings
import com.tangem.domain.tokens.model.warnings.KaspaWarnings
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.transaction.models.AssetRequirementsCondition
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList", "SuspendFunWithFlowReturnType")
internal class GetCurrencyWarningsUseCase @Inject constructor(
    private val walletManagersFacade: WalletManagersFacade,
    private val currenciesRepository: CurrenciesRepository,
    private val dispatchers: CoroutineDispatcherProvider,
    private val currencyChecksRepository: CurrencyChecksRepository,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currencyStatus: CryptoCurrencyStatus,
        derivationPath: Network.DerivationPath,
    ): Flow<Set<CryptoCurrencyWarning>> {
        val currency = currencyStatus.currency

        // don't add here notifications that require async requests
        return combine(
            flow = getCoinRelatedWarnings(
                userWalletId = userWalletId,
                currency = currency,
            ),
            flow2 = flowOf(currencyChecksRepository.getRentInfoWarning(userWalletId, currencyStatus)),
            flow3 = flowOf(currencyChecksRepository.getExistentialDeposit(userWalletId, currency.network)),
            flow4 = flowOf(currencyChecksRepository.getFeeResourceAmount(userWalletId, currency.network)),
        ) { coinRelatedWarnings, maybeRentWarning, maybeEdWarning, maybeFeeResource ->
            setOfNotNull(
                maybeRentWarning,
                maybeEdWarning?.let { getExistentialDepositWarning(currency, it) },
                maybeFeeResource?.let { getFeeResourceWarning(it) },
                *coinRelatedWarnings.toTypedArray(),
                getNetworkUnavailableWarning(currencyStatus),
                getNetworkNoAccountWarning(currencyStatus),
                getBeaconChainShutdownWarning(rawId = currency.network.id.rawId),
                getAssetRequirementsWarning(userWalletId = userWalletId, currency = currency),
                getMigrationFromMaticToPolWarning(currency),
                getCloreMigrationWarning(currency),
            )
        }.flowOn(dispatchers.io)
    }

    private suspend fun getCoinRelatedWarnings(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
    ): Flow<List<CryptoCurrencyWarning>> {
        return singleAccountStatusListSupplier(userWalletId)
            .map { accountStatusList ->
                val coin = accountStatusList.getCoinStatus(currency).getOrNull()
                val token = accountStatusList.getCryptoCurrencyStatus(currency).getOrNull()

                coin to token
            }
            .distinctUntilChanged()
            .map { pair ->
                val (coinStatus, tokenStatus) = pair

                if (tokenStatus != null && coinStatus != null) {
                    listOfNotNull(
                        getUsedOutdatedDataWarning(tokenStatus),
                        getFeeWarning(
                            userWalletId = userWalletId,
                            coinStatus = coinStatus,
                            tokenStatus = tokenStatus,
                        ),
                    )
                } else {
                    listOf(CryptoCurrencyWarning.SomeNetworksUnreachable)
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
        val isNetworkSupportGasless =
            currencyChecksRepository.isNetworkSupportedForGaslessTx(coinStatus.currency.network)
        return when {
            feePaidCurrency is FeePaidCurrency.Coin &&
                !tokenStatus.value.amount.isZero() &&
                coinStatus.value.amount.isZero() &&
                !isNetworkFeeZero &&
                !isNetworkSupportGasless -> {
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
        val tokens = multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
            params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
        )
            .orEmpty()

        val token = tokens.find { currency ->
            currency is CryptoCurrency.Token &&
                currency.contractAddress.equals(feePaidToken.contractAddress, ignoreCase = true) &&
                currency.network.derivationPath == tokenStatus.currency.network.derivationPath
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
        return (currencyStatus.value as? CryptoCurrencyStatus.NoAccount)?.let { noAccountStatus ->
            if (isNeedToCreateAccountWithoutReserve(networkId = currencyStatus.currency.network.rawId)) {
                CryptoCurrencyWarning.TopUpWithoutReserve
            } else {
                CryptoCurrencyWarning.SomeNetworksNoAccount(
                    amountToCreateAccount = noAccountStatus.amountToCreateAccount,
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

    private fun getCloreMigrationWarning(currency: CryptoCurrency): CryptoCurrencyWarning? {
        return if (currency.symbol == CLORE_SYMBOL && BlockchainUtils.isClore(currency.network.rawId)) {
            CryptoCurrencyWarning.MigrationClore
        } else {
            null
        }
    }

    private fun BigDecimal?.isZero(): Boolean {
        return this?.signum() == 0
    }

    companion object {
        private const val MATIC_SYMBOL = "MATIC"
        private const val CLORE_SYMBOL = "CLORE"
    }
}