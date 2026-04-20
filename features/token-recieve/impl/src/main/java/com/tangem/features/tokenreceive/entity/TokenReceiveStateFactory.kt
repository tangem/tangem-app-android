package com.tangem.features.tokenreceive.entity

import androidx.compose.ui.graphics.Color
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.*
import com.tangem.domain.models.ReceiveAddressModel
import com.tangem.domain.models.TokenReceiveNotification
import com.tangem.domain.models.TokenReceiveType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.ens.EnsAddress
import com.tangem.features.tokenreceive.entity.ReceiveAddress.Type.Ens
import com.tangem.features.tokenreceive.entity.ReceiveAddress.Type.Primary
import com.tangem.features.tokenreceive.ui.state.TokenReceiveUM
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList

@Suppress("MagicNumber")
internal class TokenReceiveStateFactory(
    private val currentStateProvider: Provider<TokenReceiveUM>,
    private val cryptoCurrency: CryptoCurrency,
    private val addresses: List<ReceiveAddressModel>,
    private val tokenReceiveNotification: List<TokenReceiveNotification>,
    private val tokenReceiveType: TokenReceiveType,
) {

    private val iconStateConverter by lazy(::CryptoCurrencyToIconStateConverter)

    fun getInitialState(tokenName: String): TokenReceiveUM {
        return TokenReceiveUM(
            addresses = mapAddresses(
                addresses = addresses,
                cryptoCurrency = cryptoCurrency,
            ),
            iconState = when (tokenReceiveType) {
                is TokenReceiveType.Default -> iconStateConverter.convert(cryptoCurrency)
                is TokenReceiveType.Custom -> getCustomCurrencyIconState(tokenReceiveType)
            },
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
                        is Primary.Dynamic -> 1
                        is Primary.Default -> 2
                        is Primary.Legacy -> 3
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
        val shouldUseToLegacyAndDefaultName = addresses.any { it.displayType == ReceiveAddressModel.DisplayType.Legacy }

        val receiveAddresses = addresses.map { model ->
            val type = when (model.displayType) {
                ReceiveAddressModel.DisplayType.Default -> {
                    Primary.Default(displayName = defaultDisplayName(cryptoCurrency, shouldUseToLegacyAndDefaultName))
                }
                ReceiveAddressModel.DisplayType.Dynamic -> {
                    Primary.Dynamic(displayName = coinAddressDisplayName(cryptoCurrency))
                }
                ReceiveAddressModel.DisplayType.Ens -> Ens
                ReceiveAddressModel.DisplayType.Legacy -> Primary.Legacy(
                    displayName = resourceReference(
                        R.string.domain_receive_assets_legacy_address,
                        WrappedList(listOf(cryptoCurrency.name)),
                    ),
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
                    is Primary.Dynamic -> 1
                    is Primary.Default -> 2
                    is Primary.Legacy -> 3
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
            tokenReceiveNotification.filter { !it.isYieldSupplyNotification }.map { notification ->
                add(
                    NotificationUM.Warning(
                        title = resourceReference(notification.title),
                        subtitle = resourceReference(notification.subtitle),
                    ),
                )
            }
        }
    }

    private fun getCustomCurrencyIconState(type: TokenReceiveType.Custom) = CurrencyIconState.TokenIcon(
        url = type.tokenIconUrl,
        topBadgeIconResId = cryptoCurrency.networkIconResId,
        fallbackTint = Color(type.fallbackTint),
        fallbackBackground = Color(type.fallbackBackground),
        isGrayscale = false,
        shouldShowCustomBadge = false,
    )

    private fun defaultDisplayName(
        cryptoCurrency: CryptoCurrency,
        needUseToLegacyAndDefaultName: Boolean,
    ): TextReference {
        return if (needUseToLegacyAndDefaultName) {
            TextReference.Res(R.string.domain_receive_assets_default_address)
        } else {
            coinAddressDisplayName(cryptoCurrency)
        }
    }

    private fun coinAddressDisplayName(cryptoCurrency: CryptoCurrency): TextReference {
        val displayName = when (cryptoCurrency) {
            is CryptoCurrency.Coin -> cryptoCurrency.name
            is CryptoCurrency.Token -> cryptoCurrency.symbol
        }
        return TextReference.Combined(
            wrappedList(
                TextReference.Str(displayName),
                TextReference.Str(" "),
                TextReference.Res(R.string.common_address),
            ),
        )
    }
}