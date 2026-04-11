package com.tangem.domain.transaction.usecase

import com.tangem.domain.dynamicaddresses.DynamicAddressesFeatureToggles
import com.tangem.domain.dynamicaddresses.GetDynamicReceiveAddressUseCase
import com.tangem.domain.dynamicaddresses.model.DynamicAddressesStatus
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.models.Asset
import com.tangem.domain.models.ReceiveAddressModel
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.TokenReceiveNotification
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.GetViewedTokenReceiveWarningUseCase
import com.tangem.domain.transaction.R
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.firstOrNull

class ReceiveAddressesFactory(
    private val getEnsNameUseCase: GetEnsNameUseCase,
    private val getViewedTokenReceiveWarningUseCase: GetViewedTokenReceiveWarningUseCase,
    private val getDynamicReceiveAddressUseCase: GetDynamicReceiveAddressUseCase,
    private val dynamicAddressesRepository: DynamicAddressesRepository,
    private val dynamicAddressesFeatureToggles: DynamicAddressesFeatureToggles,
) {

    suspend fun create(
        status: CryptoCurrencyStatus,
        userWalletId: UserWalletId,
        notifications: List<TokenReceiveNotification> = emptyList(),
    ): TokenReceiveConfig? {
        val addresses = status.value.networkAddress ?: return null
        val cryptoCurrency = status.currency

        val ensName = getEnsNameUseCase.invoke(
            userWalletId = userWalletId,
            network = cryptoCurrency.network,
            address = addresses.defaultAddress.value,
        )

        val dynamicAddress = getDynamicAddressIfEnabled(userWalletId, cryptoCurrency)

        val receiveAddresses = if (dynamicAddress != null) {
            buildDynamicAddressList(ensName, dynamicAddress)
        } else {
            buildStandardAddressList(ensName, addresses)
        }

        return TokenReceiveConfig(
            shouldShowWarning = cryptoCurrency.name !in getViewedTokenReceiveWarningUseCase(),
            cryptoCurrency = cryptoCurrency,
            userWalletId = userWalletId,
            showMemoDisclaimer = cryptoCurrency.network.transactionExtrasType != Network
                .TransactionExtrasType.NONE,
            receiveAddress = receiveAddresses,
            tokenReceiveNotification = notifications,
        )
    }

    private suspend fun getDynamicAddressIfEnabled(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): String? {
        if (!dynamicAddressesFeatureToggles.isDynamicAddressesEnabled) return null
        if (cryptoCurrency !is CryptoCurrency.Coin) return null

        val status = dynamicAddressesRepository.getStatus(userWalletId, cryptoCurrency.network).firstOrNull()
        if (status != DynamicAddressesStatus.ENABLED) return null

        return getDynamicReceiveAddressUseCase(userWalletId, cryptoCurrency.network)
            .onLeft { TangemLogger.e("Failed to get dynamic receive address: ${it.message}") }
            .getOrNull()
    }

    private fun buildDynamicAddressList(ensName: String?, dynamicAddress: String): List<ReceiveAddressModel> =
        buildList {
            ensName?.let { ens ->
                add(ReceiveAddressModel(displayType = ReceiveAddressModel.DisplayType.Ens, value = ens))
            }
            add(
                ReceiveAddressModel(
                    displayType = ReceiveAddressModel.DisplayType.Dynamic,
                    value = dynamicAddress,
                ),
            )
        }

    private fun buildStandardAddressList(ensName: String?, addresses: NetworkAddress): List<ReceiveAddressModel> =
        buildList {
            ensName?.let { ens ->
                add(ReceiveAddressModel(displayType = ReceiveAddressModel.DisplayType.Ens, value = ens))
            }
            addresses.availableAddresses.map { address ->
                add(
                    ReceiveAddressModel(
                        displayType = when (address.type) {
                            NetworkAddress.Address.Type.Primary -> ReceiveAddressModel.DisplayType.Default
                            NetworkAddress.Address.Type.Secondary -> ReceiveAddressModel.DisplayType.Legacy
                        },
                        value = address.value,
                    ),
                )
            }
        }

    suspend fun createForNft(
        userWalletId: UserWalletId,
        addresses: NetworkAddress,
        network: Network,
        nft: CryptoCurrency,
    ): TokenReceiveConfig {
        val cryptoCurrency = nft

        val ensName = getEnsNameUseCase.invoke(
            userWalletId = userWalletId,
            network = network,
            address = addresses.defaultAddress.value,
        )

        val receiveAddresses = buildList {
            ensName?.let { ens ->
                add(
                    ReceiveAddressModel(
                        displayType = ReceiveAddressModel.DisplayType.Ens,
                        value = ens,
                    ),
                )
            }
            addresses.availableAddresses.map { address ->
                add(
                    ReceiveAddressModel(
                        displayType = when (address.type) {
                            NetworkAddress.Address.Type.Primary -> ReceiveAddressModel.DisplayType.Default
                            NetworkAddress.Address.Type.Secondary -> ReceiveAddressModel.DisplayType.Legacy
                        },
                        value = address.value,
                    ),
                )
            }
        }

        val notifications = buildList {
            if (BlockchainUtils.isSolana(network.rawId)) {
                add(
                    TokenReceiveNotification(
                        title = R.string.nft_receive_unsupported_types,
                        subtitle = R.string.nft_receive_unsupported_types_description,
                    ),
                )
            }
        }

        return TokenReceiveConfig(
            shouldShowWarning = Asset.NFT.name !in getViewedTokenReceiveWarningUseCase(),
            cryptoCurrency = cryptoCurrency,
            userWalletId = userWalletId,
            showMemoDisclaimer = false,
            receiveAddress = receiveAddresses,
            tokenReceiveNotification = notifications,
            asset = Asset.NFT,
        )
    }
}