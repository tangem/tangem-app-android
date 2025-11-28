package com.tangem.domain.nft

import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.PortfolioId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.nft.models.NFTNetworks
import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.tokens.repository.CurrenciesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull

class GetNFTNetworksUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val nftRepository: NFTRepository,
) {
    operator fun invoke(portfolioId: PortfolioId): Flow<NFTNetworks> = when (portfolioId) {
        is PortfolioId.Account -> singleAccountStatusListSupplier(portfolioId.userWalletId)
            .map { it.accountStatuses }
            .mapNotNull { accountStatuses -> accountStatuses.find { it.account.accountId == portfolioId.accountId } }
            .map { accountStatus -> accountStatus.flattenCurrencies().map { it.currency } }
            .mapLatest { it.toNFTNetworks(portfolioId.userWalletId) }
        is PortfolioId.Wallet ->
            currenciesRepository
                .getWalletCurrenciesUpdates(portfolioId.userWalletId)
                .map { cryptoCurrencies -> cryptoCurrencies.toNFTNetworks(portfolioId.userWalletId) }
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