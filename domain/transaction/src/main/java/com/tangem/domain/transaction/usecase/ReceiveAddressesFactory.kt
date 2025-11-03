package com.tangem.domain.transaction.usecase

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

class ReceiveAddressesFactory(
    private val getEnsNameUseCase: GetEnsNameUseCase,
    private val getViewedTokenReceiveWarningUseCase: GetViewedTokenReceiveWarningUseCase,
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

        val receiveAddresses = buildList {
            ensName?.let { ens ->
                add(
                    ReceiveAddressModel(
                        nameService = ReceiveAddressModel.NameService.Ens,
                        value = ens,
                    ),
                )
            }
            addresses.availableAddresses.map { address ->
                add(
                    ReceiveAddressModel(
                        nameService = when (address.type) {
                            NetworkAddress.Address.Type.Primary -> ReceiveAddressModel.NameService.Default
                            NetworkAddress.Address.Type.Secondary -> ReceiveAddressModel.NameService.Legacy
                        },
                        value = address.value,
                    ),
                )
            }
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
                        nameService = ReceiveAddressModel.NameService.Ens,
                        value = ens,
                    ),
                )
            }
            addresses.availableAddresses.map { address ->
                add(
                    ReceiveAddressModel(
                        nameService = when (address.type) {
                            NetworkAddress.Address.Type.Primary -> ReceiveAddressModel.NameService.Default
                            NetworkAddress.Address.Type.Secondary -> ReceiveAddressModel.NameService.Legacy
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