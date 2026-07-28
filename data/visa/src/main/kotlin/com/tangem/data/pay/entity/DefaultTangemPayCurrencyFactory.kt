package com.tangem.data.pay.entity

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.domain.card.common.visa.VisaUtilities
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultTangemPayCurrencyFactory @Inject constructor(
    excludedBlockchains: ExcludedBlockchains,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val networkFactory: NetworkFactory,
) : TangemPayCurrencyFactory {
    private val cryptoCurrencyFactory by lazy(mode = LazyThreadSafetyMode.NONE) {
        CryptoCurrencyFactory(excludedBlockchains)
    }

    override fun create(userWalletId: UserWalletId): CryptoCurrency.Token {
        val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
        val network = networkFactory.create(
            blockchain = VisaUtilities.visaBlockchain,
            userWallet = userWallet,
            extraDerivationPath = null,
        )
        return cryptoCurrencyFactory.createToken(
            network = requireNotNull(network),
            rawId = TangemPayCurrencyFactory.TOKEN_ID,
            name = TangemPayCurrencyFactory.TOKEN_NAME,
            symbol = TangemPayCurrencyFactory.TOKEN_NAME,
            contractAddress = TangemPayCurrencyFactory.TOKEN_CONTRACT_ADDRESS,
            decimals = TangemPayCurrencyFactory.TOKEN_DECIMALS,
        )
    }

    override fun createVirtualAccountToken(userWalletId: UserWalletId): CryptoCurrency.Token {
        val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
        val network = networkFactory.create(
            blockchain = VisaUtilities.visaBlockchain,
            derivationPath = Network.DerivationPath.Custom(VisaUtilities.virtualAccountDerivationPath.rawPath),
            userWallet = userWallet,
        )
        return cryptoCurrencyFactory.createToken(
            network = requireNotNull(network),
            rawId = TangemPayCurrencyFactory.TOKEN_ID,
            name = TangemPayCurrencyFactory.TOKEN_NAME,
            symbol = TangemPayCurrencyFactory.TOKEN_NAME,
            contractAddress = TangemPayCurrencyFactory.TOKEN_CONTRACT_ADDRESS,
            decimals = TangemPayCurrencyFactory.TOKEN_DECIMALS,
        )
    }

    override fun createNetworkStatuses(
        userWalletId: UserWalletId,
        networks: List<CustomerInfo.NetworkInfo>,
        fiatRate: BigDecimal?,
    ): List<PaymentNetworkStatus> {
        if (networks.isEmpty()) return emptyList()
        val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
        return networks.mapNotNull { networkInfo ->
            val blockchain = PaymentNetworkBlockchainResolver.blockchainFor(
                name = networkInfo.name,
                chainId = networkInfo.chainId,
                isTestnet = networkInfo.isTestnet,
            ) ?: return@mapNotNull null
            val network = networkFactory.create(
                blockchain = blockchain,
                extraDerivationPath = null,
                userWallet = userWallet,
            ) ?: return@mapNotNull null
            networkInfo.toPaymentNetworkStatus(network, blockchain, fiatRate)
        }
    }

    private fun CustomerInfo.NetworkInfo.toPaymentNetworkStatus(
        network: Network,
        blockchain: Blockchain,
        fiatRate: BigDecimal?,
    ): PaymentNetworkStatus {
        val currencies = tokens.map { token -> createToken(network, blockchain, token) }
        return when (status) {
            CustomerInfo.NetworkInfo.Status.ENABLED -> PaymentNetworkStatus.Available(
                network = network,
                depositAddress = depositAddress.orEmpty(),
                cryptoCurrencyStatuses = tokens.zip(currencies).map { (token, currency) ->
                    buildStatus(
                        currency = currency,
                        amount = token.availableForWithdrawal.orZero(),
                        fiatRate = fiatRate,
                        depositAddress = depositAddress.orEmpty(),
                    )
                },
            )
            CustomerInfo.NetworkInfo.Status.NOT_ISSUED -> PaymentNetworkStatus.NotIssued(
                network = network,
                cryptoCurrencies = currencies,
            )
            CustomerInfo.NetworkInfo.Status.DISABLED -> PaymentNetworkStatus.Disabled(
                network = network,
                cryptoCurrencies = currencies,
            )
        }
    }

    private fun createToken(
        network: Network,
        blockchain: Blockchain,
        token: CustomerInfo.NetworkInfo.Token,
    ): CryptoCurrency.Token {
        return cryptoCurrencyFactory.createToken(
            network = network,
            rawId = rawIdFor(token.symbol),
            name = token.symbol,
            symbol = token.symbol,
            contractAddress = token.contractAddress,
            decimals = PaymentTokenDecimalsResolver.decimalsFor(blockchain),
        )
    }

    private fun buildStatus(
        currency: CryptoCurrency.Token,
        amount: BigDecimal,
        fiatRate: BigDecimal?,
        depositAddress: String,
    ): CryptoCurrencyStatus {
        val networkAddress = NetworkAddress.Single(
            defaultAddress = NetworkAddress.Address(
                type = NetworkAddress.Address.Type.Primary,
                value = depositAddress,
            ),
        )
        val value = if (fiatRate != null) {
            CryptoCurrencyStatus.Loaded(
                amount = amount,
                fiatAmount = amount.multiply(fiatRate),
                fiatRate = fiatRate,
                priceChange = BigDecimal.ZERO,
                networkAddress = networkAddress,
                sources = CryptoCurrencyStatus.Sources(),
                pendingTransactions = emptySet(),
                stakingBalance = null,
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
            )
        } else {
            CryptoCurrencyStatus.NoQuote(
                amount = amount,
                networkAddress = networkAddress,
                stakingBalance = null,
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                sources = CryptoCurrencyStatus.Sources(),
            )
        }
        return CryptoCurrencyStatus(currency = currency, value = value)
    }

    private fun rawIdFor(symbol: String): CryptoCurrency.RawID? = when (symbol.uppercase(Locale.US)) {
        "USDC" -> CryptoCurrency.RawID("usd-coin")
        "USDT" -> CryptoCurrency.RawID("tether")
        else -> null
    }
}