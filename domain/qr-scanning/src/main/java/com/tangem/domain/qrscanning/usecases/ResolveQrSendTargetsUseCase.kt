package com.tangem.domain.qrscanning.usecases

import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.networks.repository.NetworksRepository
import com.tangem.domain.qrscanning.models.ClassifiedQrContent
import com.tangem.domain.qrscanning.repository.QrScanningEventsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import com.tangem.domain.qrscanning.models.QrSendTarget
import kotlinx.coroutines.awaitAll

class ResolveQrSendTargetsUseCase(
    private val multiAccountListSupplier: MultiAccountListSupplier,
    private val qrScanningEventsRepository: QrScanningEventsRepository,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val networksRepository: NetworksRepository,
) {

    suspend operator fun invoke(qrCode: String): QrSendTarget {
        val allAccountLists = multiAccountListSupplier.getSyncOrNull(Unit).orEmpty()
        val userWallets = userWalletsListRepository.userWalletsSync()
        val walletNamesMap = userWallets.associate { it.walletId to it.name }

        val allCurrencies = mutableListOf<CryptoCurrency>()
        val currencyLocations = mutableMapOf<CryptoCurrency.ID, MutableList<CurrencyLocation>>()
        val totalPerAccount = mutableMapOf<AccountId, Int>()

        val allAccounts = allAccountLists.flatMap { it.accounts }.filterIsInstance<Account.CryptoPortfolio>()

        for (account in allAccounts) {
            val location = CurrencyLocation(
                walletName = walletNamesMap[account.accountId.userWalletId]
                    ?: account.accountId.userWalletId.stringValue,
                accountId = account.accountId,
                accountName = account.accountName,
            )
            totalPerAccount[account.accountId] = account.cryptoCurrencies.size
            for (currency in account.cryptoCurrencies) {
                allCurrencies.add(currency)
                currencyLocations.getOrPut(currency.id) { mutableListOf() }.add(location)
            }
        }

        val classified = qrScanningEventsRepository.classify(qrCode, allCurrencies)
        val portfolioIndex = PortfolioIndex(currencyLocations, totalPerAccount)

        return resolve(classified, portfolioIndex)
    }

    private suspend fun resolve(classified: ClassifiedQrContent, portfolioIndex: PortfolioIndex): QrSendTarget {
        return when (classified) {
            is ClassifiedQrContent.WalletConnect -> QrSendTarget.WalletConnect(classified.uri)
            is ClassifiedQrContent.Error -> QrSendTarget.Error(classified)
            is ClassifiedQrContent.PlainAddress -> resolveAddressTarget(
                address = classified.address,
                amount = null,
                memo = null,
                matchingCurrencies = classified.matchingCurrencies,
                portfolioIndex = portfolioIndex,
            )
            is ClassifiedQrContent.PaymentUri -> resolveAddressTarget(
                address = classified.address,
                amount = classified.amount,
                memo = classified.memo,
                matchingCurrencies = classified.matchingCurrencies,
                portfolioIndex = portfolioIndex,
            )
            is ClassifiedQrContent.PaymentUriWarning -> {
                val inner = resolve(classified.paymentUri, portfolioIndex)
                QrSendTarget.Warning(
                    target = inner,
                    unsupportedParams = classified.unsupportedParams,
                )
            }
        }
    }

    private suspend fun resolveAddressTarget(
        address: String,
        amount: BigDecimal?,
        memo: String?,
        matchingCurrencies: List<CryptoCurrency>,
        portfolioIndex: PortfolioIndex,
    ): QrSendTarget {
        val ownAddressNetworks = findOwnAddressNetworks(address, matchingCurrencies, portfolioIndex)
        val walletGroups = buildWalletGroups(matchingCurrencies, portfolioIndex, ownAddressNetworks)

        if (walletGroups.isEmpty()) {
            return QrSendTarget.AddressSameAsWallet
        }

        val singleGroup = walletGroups.singleOrNull()
        val singleCurrency = singleGroup?.accounts?.singleOrNull()?.currencies?.singleOrNull()

        return if (singleGroup != null && singleCurrency != null) {
            QrSendTarget.Single(
                userWalletId = singleGroup.userWalletId,
                currency = singleCurrency,
                address = address,
                amount = amount,
                memo = memo,
            )
        } else {
            QrSendTarget.Multiple(
                address = address,
                amount = amount,
                memo = memo,
                walletGroups = walletGroups,
            )
        }
    }

    private suspend fun findOwnAddressNetworks(
        address: String,
        matchingCurrencies: List<CryptoCurrency>,
        portfolioIndex: PortfolioIndex,
    ): Map<UserWalletId, List<Network.ID>> = coroutineScope {
        matchingCurrencies.distinctBy { it.id }
            .flatMap { currency ->
                portfolioIndex.currencyLocations[currency.id].orEmpty().map {
                    it.accountId.userWalletId to currency.network
                }
            }
            .distinct()
            .map { (walletId, network) ->
                async {
                    val ownAddress = networksRepository.getDefaultAddress(walletId, network)
                    if (ownAddress == address) walletId to network else null
                }
            }
            .awaitAll()
            .filterNotNull()
            .groupBy(keySelector = { it.first }, valueTransform = { it.second.id })
    }

    private fun buildWalletGroups(
        matchingCurrencies: List<CryptoCurrency>,
        portfolioIndex: PortfolioIndex,
        ownAddressNetworks: Map<UserWalletId, List<Network.ID>>,
    ): List<QrSendTarget.Multiple.WalletGroup> {
        val walletMap = linkedMapOf<UserWalletId, WalletInfo>()
        val uniqueCurrencies = matchingCurrencies.distinctBy { it.id }

        for (currency in uniqueCurrencies) {
            val locations = portfolioIndex.currencyLocations[currency.id].orEmpty()
            for (location in locations) {
                val walletId = location.accountId.userWalletId
                val isOwnAddress = ownAddressNetworks[walletId]?.contains(currency.network.id) == true

                if (!isOwnAddress) {
                    val walletInfo = walletMap.getOrPut(walletId) {
                        WalletInfo(location.walletName, linkedMapOf())
                    }
                    val accountInfo = walletInfo.accounts.getOrPut(location.accountId) {
                        AccountInfo(location.accountName, mutableListOf())
                    }
                    accountInfo.currencies.add(currency)
                }
            }
        }

        return walletMap.map { (walletId, walletInfo) ->
            QrSendTarget.Multiple.WalletGroup(
                userWalletId = walletId,
                walletName = walletInfo.walletName,
                accounts = walletInfo.accounts.map { (accountId, accountInfo) ->
                    val total = portfolioIndex.totalPerAccount[accountId] ?: 0
                    QrSendTarget.Multiple.AccountGroup(
                        accountId = accountId,
                        accountName = accountInfo.accountName,
                        currencies = accountInfo.currencies,
                        hiddenTokensCount = total - accountInfo.currencies.size,
                    )
                },
            )
        }
    }

    private class PortfolioIndex(
        val currencyLocations: Map<CryptoCurrency.ID, List<CurrencyLocation>>,
        val totalPerAccount: Map<AccountId, Int>,
    )

    private data class CurrencyLocation(
        val walletName: String,
        val accountId: AccountId,
        val accountName: AccountName,
    )

    private class WalletInfo(
        val walletName: String,
        val accounts: LinkedHashMap<AccountId, AccountInfo>,
    )

    private class AccountInfo(
        val accountName: AccountName,
        val currencies: MutableList<CryptoCurrency>,
    )
}