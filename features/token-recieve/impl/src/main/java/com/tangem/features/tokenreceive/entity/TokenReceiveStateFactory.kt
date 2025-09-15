package com.tangem.features.tokenreceive.entity

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.ReceiveAddressModel
import com.tangem.domain.models.TokenReceiveNotification
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.ens.EnsAddress
import com.tangem.features.tokenreceive.entity.ReceiveAddress.Type.Ens
import com.tangem.features.tokenreceive.entity.ReceiveAddress.Type.Primary
import com.tangem.features.tokenreceive.ui.state.TokenReceiveUM
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList

internal class TokenReceiveStateFactory(
    private val currentStateProvider: Provider<TokenReceiveUM>,
    private val cryptoCurrency: CryptoCurrency,
    private val addresses: List<ReceiveAddressModel>,
    private val tokenReceiveNotification: List<TokenReceiveNotification>,
) {

    private val iconStateConverter by lazy(::CryptoCurrencyToIconStateConverter)

    fun getInitialState(tokenName: String): TokenReceiveUM {
        return TokenReceiveUM(
            addresses = mapAddresses(
                addresses = addresses,
                cryptoCurrency = cryptoCurrency,
            ),
            iconState = iconStateConverter.convert(cryptoCurrency),
            network = cryptoCurrency.network.name,
            isEnsResultLoading = false,
            notificationConfigs = getNotifications(
                tokenName = tokenName,
                tokenReceiveNotification = tokenReceiveNotification,
            ).toImmutableList(),
        )
    }

    fun getLoadingState(): TokenReceiveUM {
        return currentStateProvider.invoke().copy(isEnsResultLoading = true)
    }

    fun getLoadedState(ensAddress: List<EnsAddress>): TokenReceiveUM {
        val currentAddressValues = currentStateProvider.invoke().addresses.map { it.value }.toSet()

        val newEnsAddresses = ensAddress
            .filterIsInstance<EnsAddress.Address>()
            .filterNot { it.name in currentAddressValues }
            .map { ensAddress -> ReceiveAddress(value = ensAddress.name, type = Ens) }

        val combinedAddresses = (currentStateProvider.invoke().addresses + newEnsAddresses)
            .sortedWith(
                compareBy { address ->
                    when (address.type) {
                        is Ens -> 0
                        is Primary.Default -> 1
                        is Primary.Legacy -> 2
                    }
                },
            )

        return currentStateProvider.invoke().copy(
            isEnsResultLoading = false,
            addresses = combinedAddresses.toPersistentList(),
        )
    }

    private fun mapAddresses(
        addresses: List<ReceiveAddressModel>,
        cryptoCurrency: CryptoCurrency,
    ): ImmutableList<ReceiveAddress> {
        val needUseToLegacyAndDefaultName = addresses.any { it.nameService == ReceiveAddressModel.NameService.Legacy }

        val receiveAddresses = addresses.map { model ->
            val type = when (model.nameService) {
                ReceiveAddressModel.NameService.Default -> {
                    val displayName = when (cryptoCurrency) {
                        is CryptoCurrency.Coin -> cryptoCurrency.name
                        is CryptoCurrency.Token -> cryptoCurrency.symbol
                    }

                    Primary.Default(
                        displayName = if (needUseToLegacyAndDefaultName) {
                            TextReference.Res(R.string.domain_receive_assets_default_address)
                        } else {
                            TextReference.Combined(
                                wrappedList(
                                    TextReference.Str(displayName),
                                    TextReference.Str(" "),
                                    TextReference.Res(R.string.common_address),
                                ),
                            )
                        },
                    )
                }
                ReceiveAddressModel.NameService.Ens -> Ens
                ReceiveAddressModel.NameService.Legacy -> Primary.Legacy(
                    displayName = TextReference.Res(R.string.domain_receive_assets_legacy_address),
                )
            }
            ReceiveAddress(
                value = model.value,
                type = type,
            )
        }

        val sortedAddresses = receiveAddresses.sortedWith(
            compareBy { address ->
                when (address.type) {
                    is Ens -> 0
                    is Primary.Default -> 1
                    is Primary.Legacy -> 2
                }
            },
        )

        return sortedAddresses.toPersistentList()
    }

    private fun getNotifications(
        tokenName: String,
        tokenReceiveNotification: List<TokenReceiveNotification>,
    ): List<NotificationUM> {
        return buildList {
            add(
                NotificationUM.Info(
                    title = resourceReference(
                        R.string.receive_bottom_sheet_warning_title,
                        wrappedList(
                            tokenName,
                            cryptoCurrency.network.name,
                        ),
                    ),
                    subtitle = resourceReference(R.string.receive_bottom_sheet_warning_message_description),
                    iconTint = NotificationConfig.IconTint.Accent,
                ),
            )
            tokenReceiveNotification.map { notification ->
                add(
                    NotificationUM.Warning(
                        title = resourceReference(notification.title),
                        subtitle = resourceReference(notification.subtitle),
                    ),
                )
            }
        }
    }
}