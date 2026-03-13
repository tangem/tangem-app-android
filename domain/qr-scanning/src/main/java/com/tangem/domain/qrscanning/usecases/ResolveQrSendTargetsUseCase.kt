package com.tangem.domain.qrscanning.usecases

import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.qrscanning.models.ClassifiedQrContent
import com.tangem.domain.qrscanning.repository.QrScanningEventsRepository
import java.math.BigDecimal
import com.tangem.domain.qrscanning.models.QrSendTarget

class ResolveQrSendTargetsUseCase(
    private val multiAccountListSupplier: MultiAccountListSupplier,
    private val qrScanningEventsRepository: QrScanningEventsRepository,
) {

    suspend operator fun invoke(qrCode: String): QrSendTarget {
        val allAccountLists = multiAccountListSupplier.getSyncOrNull(Unit).orEmpty()

        val currencyEntries = allAccountLists.flatMap { accountList ->
            accountList.accounts
                .filterIsInstance<Account.CryptoPortfolio>()
                .flatMap { account ->
                    account.cryptoCurrencies.map { currency ->
                        currency to CurrencyLocation(
                            userWalletId = accountList.userWalletId,
                            walletName = accountList.userWalletId.stringValue,
                            accountId = account.accountId,
                            accountName = account.accountName,
                        )
                    }
                }
        }

        val allCurrencies = currencyEntries.map { it.first }
        val currencyLocations = currencyEntries.groupBy(
            keySelector = { it.first.id },
            valueTransform = { it.second },
        )

        val classified = qrScanningEventsRepository.classify(qrCode, allCurrencies)

        return resolve(classified, currencyLocations)
    }

    private fun resolve(
        classified: ClassifiedQrContent,
        currencyLocations: Map<CryptoCurrency.ID, List<CurrencyLocation>>,
    ): QrSendTarget {
        return when (classified) {
            is ClassifiedQrContent.WalletConnect -> QrSendTarget.WalletConnect(classified.uri)
            is ClassifiedQrContent.Unknown -> QrSendTarget.Unknown(classified.raw)
            is ClassifiedQrContent.PlainAddress -> resolveAddressTarget(
                address = classified.address,
                amount = null,
                memo = null,
                matchingCurrencies = classified.matchingCurrencies,
                currencyLocations = currencyLocations,
            )
            is ClassifiedQrContent.PaymentUri -> resolveAddressTarget(
                address = classified.address,
                amount = classified.amount,
                memo = classified.memo,
                matchingCurrencies = classified.matchingCurrencies,
                currencyLocations = currencyLocations,
            )
        }
    }

    private fun resolveAddressTarget(
        address: String,
        amount: BigDecimal?,
        memo: String?,
        matchingCurrencies: List<CryptoCurrency>,
        currencyLocations: Map<CryptoCurrency.ID, List<CurrencyLocation>>,
    ): QrSendTarget {
        val walletGroups = buildWalletGroups(matchingCurrencies, currencyLocations)

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

    private fun buildWalletGroups(
        matchingCurrencies: List<CryptoCurrency>,
        currencyLocations: Map<CryptoCurrency.ID, List<CurrencyLocation>>,
    ): List<QrSendTarget.Multiple.WalletGroup> {
        val walletMap = linkedMapOf<UserWalletId, WalletInfo>()
        val uniqueCurrencies = matchingCurrencies.distinctBy { it.id }

        for (currency in uniqueCurrencies) {
            val locations = currencyLocations[currency.id] ?: continue
            for (location in locations) {
                val walletInfo = walletMap.getOrPut(location.userWalletId) {
                    WalletInfo(location.walletName, linkedMapOf())
                }
                val accountInfo = walletInfo.accounts.getOrPut(location.accountId) {
                    AccountInfo(location.accountName, mutableListOf())
                }
                accountInfo.currencies.add(currency)
            }
        }

        return walletMap.map { (walletId, walletInfo) ->
            QrSendTarget.Multiple.WalletGroup(
                userWalletId = walletId,
                walletName = walletInfo.walletName,
                accounts = walletInfo.accounts.map { (accountId, accountInfo) ->
                    QrSendTarget.Multiple.AccountGroup(
                        accountId = accountId,
                        accountName = accountInfo.accountName,
                        currencies = accountInfo.currencies,
                    )
                },
            )
        }
    }

    private data class CurrencyLocation(
        val userWalletId: UserWalletId,
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