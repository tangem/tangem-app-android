package com.tangem.features.markets.portfolio.impl.model

import com.tangem.domain.markets.FilterAvailableNetworksForWalletUseCase
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

internal typealias WalletsWithNetworks = Map<UserWalletId, Set<TokenMarketInfo.Network>>

/**
 * Manager for tracking changing networks in AddToPortfolio
 *
[REDACTED_AUTHOR]
 */
internal class AddToPortfolioManager @Inject constructor(
    private val filterAvailableNetworksForWalletUseCase: FilterAvailableNetworksForWalletUseCase,
) {

    val availableNetworks = MutableStateFlow<Set<TokenMarketInfo.Network>?>(value = null)
    private val addedNetworks = MutableStateFlow<WalletsWithNetworks>(value = emptyMap())
    private val removedNetworks = MutableStateFlow<WalletsWithNetworks>(value = emptyMap())

    /** Get [AddToPortfolioData] as flow */
    fun getAddToPortfolioData(): Flow<AddToPortfolioData> {
        return combine(
            flow = availableNetworks,
            flow2 = addedNetworks,
            flow3 = removedNetworks,
            transform = ::AddToPortfolioData,
        )
    }

    /** Set available networks [networks] */
    fun setAvailableNetworks(networks: List<TokenMarketInfo.Network>) {
        availableNetworks.value = networks.toSet()
    }

    /** Add network [networkId] to [userWalletId] */
    fun addNetwork(userWalletId: UserWalletId, networkId: String) {
        addedNetworks.add(userWalletId, networkId)

        removedNetworks.cancelPrevChangeIfExist(userWalletId = userWalletId, networkId = networkId)
    }

    /** Remove network [networkId] from [userWalletId] */
    fun removeNetwork(userWalletId: UserWalletId, networkId: String) {
        removedNetworks.add(userWalletId, networkId)

        addedNetworks.cancelPrevChangeIfExist(
            userWalletId = userWalletId,
            networkId = networkId,
        )
    }

    /** Remove all networks by [userWalletId] */
    fun removeAllChanges(userWalletId: UserWalletId) {
        addedNetworks.update {
            it.toMutableMap().apply { remove(userWalletId) }
        }

        removedNetworks.update {
            it.toMutableMap().apply { remove(userWalletId) }
        }
    }

    fun associateWithToggle(
        userWalletId: UserWalletId,
        alreadyAddedNetworkIds: Set<String>,
        addToPortfolioData: AddToPortfolioData,
    ): Map<TokenMarketInfo.Network, Boolean> {
        val filteredNetworks = filterAvailableNetworksForWalletUseCase(
            userWalletId = userWalletId,
            networks = addToPortfolioData.availableNetworks.orEmpty(),
        )
        // Use user choice or check already added networks
        return filteredNetworks.associateWith { availableNetwork ->
            val isAddedByUser = addToPortfolioData.addedNetworks[userWalletId]?.contains(availableNetwork)

            if (isAddedByUser == true) return@associateWith true

            val isRemovedByUser = addToPortfolioData.removedNetworks[userWalletId]?.contains(availableNetwork)

            if (isRemovedByUser == true) return@associateWith false

            val isAddedBefore = alreadyAddedNetworkIds.any { it == availableNetwork.networkId }

            isAddedBefore
        }
    }

    private fun MutableStateFlow<WalletsWithNetworks>.cancelPrevChangeIfExist(
        userWalletId: UserWalletId,
        networkId: String,
    ) {
        if (value[userWalletId].orEmpty().any { it.networkId == networkId }) remove(userWalletId, networkId)
    }

    private fun MutableStateFlow<WalletsWithNetworks>.add(userWalletId: UserWalletId, networkId: String) {
        change(userWalletId = userWalletId, networkId = networkId, isAddAction = true)
    }

    private fun MutableStateFlow<WalletsWithNetworks>.remove(userWalletId: UserWalletId, networkId: String) {
        change(userWalletId = userWalletId, networkId = networkId, isAddAction = false)
    }

    private fun MutableStateFlow<WalletsWithNetworks>.change(
        userWalletId: UserWalletId,
        networkId: String,
        isAddAction: Boolean,
    ) {
        val network = availableNetworks.value.orEmpty().firstOrNull { it.networkId == networkId }

        if (network == null) {
            Timber.d(
                "Network [$networkId] doesn't contain in available networks [%s]",
                availableNetworks.value?.joinToString { it.networkId },
            )

            return
        }

        update {
            it.toMutableMap().apply {
                this[userWalletId] = if (isAddAction) {
                    this[userWalletId].orEmpty() + network
                } else {
                    this[userWalletId].orEmpty() - network
                }
            }
        }
    }

    /**
     * Add to portfolio data
     *
     * @property availableNetworks available networks that user can add to portfolio
     * @property addedNetworks     networks that user toggled on, but it might have already been added to the wallet
     * @property removedNetworks   networks that user toggled off, but it might haven't been added to the wallet
     *
     * Example for [addedNetworks] and [removedNetworks]. This lists will include new networks when user just
     * toggle it. But when we will save user changes, we will check what tokens have already been added or
     * haven't been added to the wallet. See [getAddedNetworks] and [getRemovedNetworks]
     */
    data class AddToPortfolioData(
        val availableNetworks: Set<TokenMarketInfo.Network>?,
        val addedNetworks: WalletsWithNetworks,
        val removedNetworks: WalletsWithNetworks,
    ) {

        fun isUserAddedNetworks(userWalletId: UserWalletId): Boolean {
            return addedNetworks[userWalletId].orEmpty().isNotEmpty()
        }

        fun isUserChangedNetworks(userWalletId: UserWalletId): Boolean {
            return addedNetworks[userWalletId].orEmpty().isNotEmpty() ||
                removedNetworks[userWalletId].orEmpty().isNotEmpty()
        }

        /** Get new networks that user [userWalletId] added using [alreadyAddedNetworkIds] */
        fun getAddedNetworks(
            userWalletId: UserWalletId,
            alreadyAddedNetworkIds: Set<String>,
        ): Set<TokenMarketInfo.Network> {
            val addedNetworksByUser = addedNetworks[userWalletId].orEmpty()

            return addedNetworksByUser.map { it.networkId }
                .minus(alreadyAddedNetworkIds)
                .mapNotNull { networkId -> addedNetworksByUser.firstOrNull { it.networkId == networkId } }
                .toSet()
        }

        /** Get networks that user [userWalletId] removed using [alreadyAddedNetworkIds] */
        fun getRemovedNetworks(
            userWalletId: UserWalletId,
            alreadyAddedNetworkIds: Set<String>,
        ): Set<TokenMarketInfo.Network> {
            val removedNetworksByUser = removedNetworks[userWalletId].orEmpty()

            return alreadyAddedNetworkIds
                .minus(removedNetworksByUser.map { it.networkId }.toSet())
                .mapNotNull { networkId -> removedNetworksByUser.firstOrNull { it.networkId == networkId } }
                .toSet()
        }
    }
}