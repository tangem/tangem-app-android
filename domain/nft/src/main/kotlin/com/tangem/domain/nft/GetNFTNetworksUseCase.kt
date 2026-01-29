package com.tangem.domain.nft

import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.PortfolioId
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.nft.models.NFTNetworks
import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.tokens.repository.CurrenciesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull

class GetNFTNetworksUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val nftRepository: NFTRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(portfolioId: PortfolioId): Flow<NFTNetworks> = when (portfolioId) {
        is PortfolioId.Account -> {
            singleAccountListSupplier(portfolioId.userWalletId)
                .mapNotNull { accountList ->
                    val account = accountList.accounts.find { it.accountId == portfolioId.accountId }

                    (account as? Account.Crypto)?.cryptoCurrencies?.toList()
                }
                .mapLatest { it.toNFTNetworks(portfolioId.userWalletId) }
        }
        is PortfolioId.Wallet -> {
            currenciesRepository
                .getWalletCurrenciesUpdates(portfolioId.userWalletId)
                .map { cryptoCurrencies -> cryptoCurrencies.toNFTNetworks(portfolioId.userWalletId) }
        }
    }

    private suspend fun List<CryptoCurrency>.toNFTNetworks(userWalletId: UserWalletId): NFTNetworks {
        val availableNetworks = this
            .map { cryptoCurrency -> cryptoCurrency.network }
            .filter { nftRepository.isNFTSupported(userWalletId, it) }
            .sortedBy { it.name }

        val unavailableNetworks = nftRepository
            .getNFTSupportedNetworks(userWalletId)
            .filter { supportedNetwork -> availableNetworks.none { it.id == supportedNetwork.id } }
            .sortedBy { it.name }

        return NFTNetworks(
            availableNetworks = availableNetworks,
            unavailableNetworks = unavailableNetworks,
        )
    }
}