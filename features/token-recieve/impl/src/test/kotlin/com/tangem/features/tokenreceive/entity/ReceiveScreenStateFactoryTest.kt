package com.tangem.features.tokenreceive.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.Asset
import com.tangem.domain.models.TokenReceiveType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.features.tokenreceive.ui.state.ReceiveScreenUM
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class ReceiveScreenStateFactoryTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Texts {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN asset WHEN create THEN title subtitle notice and extras hint follow the receive rules`(
            model: TextsModel,
        ) {
            // Arrange
            val factory = createFactory(
                currency = model.currency,
                asset = model.asset,
                type = model.type,
                tokenName = model.tokenName,
            )

            // Act
            val actual = factory.createScreen(addresses = listOf(defaultAddress))

            // Assert
            assertThat(actual.texts()).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<TextsModel> {
            val usdt = createToken(Blockchain.Ethereum, Network.StandardType.ERC20)
            val eth = createCoin(Blockchain.Ethereum)
            val optimism = createCoin(Blockchain.Optimism)
            val xrp = createCoin(Blockchain.XRP, extrasType = Network.TransactionExtrasType.DESTINATION_TAG)
            val xlm = createCoin(Blockchain.Stellar, extrasType = Network.TransactionExtrasType.MEMO)
            val solana = createCoin(Blockchain.Solana)
            return listOf(
                TextsModel(
                    currency = usdt,
                    tokenName = "USDT",
                    expected = ScreenTexts(
                        title = stringReference("Tether • USDT"),
                        subtitle = subtitle(usdt, label = "ERC20"),
                        networkNotice = notice("USDT", usdt, label = "ERC20"),
                        extrasHint = ReceiveExtrasHint.None,
                    ),
                ),
                TextsModel(
                    currency = eth,
                    tokenName = "ETH",
                    expected = ScreenTexts(
                        title = stringReference("${eth.name} • ETH"),
                        subtitle = subtitle(eth, label = null),
                        networkNotice = notice("ETH", eth, label = null),
                        extrasHint = ReceiveExtrasHint.None,
                    ),
                ),
                TextsModel(
                    currency = optimism,
                    tokenName = "ETH",
                    expected = ScreenTexts(
                        title = stringReference("${optimism.name} • ${optimism.symbol}"),
                        subtitle = subtitle(optimism, label = "Optimism"),
                        networkNotice = notice("ETH", optimism, label = "Optimism"),
                        extrasHint = ReceiveExtrasHint.None,
                    ),
                ),
                TextsModel(
                    currency = xrp,
                    tokenName = "XRP",
                    expected = ScreenTexts(
                        title = stringReference("${xrp.name} • ${xrp.symbol}"),
                        subtitle = subtitle(xrp, label = null),
                        networkNotice = notice("XRP", xrp, label = null),
                        extrasHint = ReceiveExtrasHint.NoTag,
                    ),
                ),
                TextsModel(
                    currency = xlm,
                    tokenName = "XLM",
                    expected = ScreenTexts(
                        title = stringReference("${xlm.name} • ${xlm.symbol}"),
                        subtitle = subtitle(xlm, label = null),
                        networkNotice = notice("XLM", xlm, label = null),
                        extrasHint = ReceiveExtrasHint.NoMemo,
                    ),
                ),
                TextsModel(
                    currency = solana,
                    asset = Asset.NFT,
                    tokenName = "NFT",
                    expected = ScreenTexts(
                        title = stringReference("NFT"),
                        subtitle = subtitle(solana, label = null),
                        networkNotice = notice("NFT", solana, label = null),
                        extrasHint = ReceiveExtrasHint.None,
                    ),
                ),
                TextsModel(
                    currency = usdt,
                    type = TokenReceiveType.Custom(
                        tokenIconUrl = "",
                        tokenName = "USDC",
                        fallbackTint = 0,
                        fallbackBackground = 0,
                    ),
                    tokenName = "USDC",
                    expected = ScreenTexts(
                        title = stringReference("USDC"),
                        subtitle = subtitle(usdt, label = "ERC20"),
                        networkNotice = notice("USDC", usdt, label = "ERC20"),
                        extrasHint = ReceiveExtrasHint.None,
                    ),
                ),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Addresses {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN receive addresses WHEN create THEN one labelled card per primary address`(model: CardsModel) {
            // Arrange
            val factory = createFactory(currency = createCoin(Blockchain.Bitcoin))

            // Act
            val actual = factory.createScreen(addresses = model.addresses)

            // Assert
            assertThat(actual.addresses.map { Card(it.value, it.label, it.isDynamic) })
                .containsExactlyElementsIn(model.expected)
                .inOrder()
        }

        private fun provideTestModels(): List<CardsModel> = listOf(
            CardsModel(
                addresses = listOf(defaultAddress),
                expected = listOf(Card(defaultAddress.value, label = null, isDynamic = false)),
            ),
            CardsModel(
                addresses = listOf(ens, defaultAddress, legacy),
                expected = listOf(
                    Card(
                        value = defaultAddress.value,
                        label = resourceReference(
                            id = R.string.domain_receive_assets_network_name_address,
                            formatArgs = wrappedList("Bitcoin"),
                        ),
                        isDynamic = false,
                    ),
                    Card(
                        value = legacy.value,
                        label = resourceReference(
                            id = R.string.domain_receive_assets_legacy_address,
                            formatArgs = wrappedList("Bitcoin"),
                        ),
                        isDynamic = false,
                    ),
                ),
            ),
            CardsModel(
                addresses = listOf(dynamic),
                expected = listOf(
                    Card(dynamic.value, label = resourceReference(R.string.dynamic_addresses), isDynamic = true),
                ),
            ),
        )

        @Test
        fun `GIVEN legacy selected WHEN copy a card and the selected address THEN copies each its own address`() {
            // Arrange
            val factory = createFactory(currency = createCoin(Blockchain.Bitcoin))
            val copied = mutableListOf<ReceiveAddress>()

            // Act
            val actual = factory.createScreen(
                addresses = listOf(ens, defaultAddress, legacy),
                selectedIndex = 1,
                onCopyAddressClick = copied::add,
                onCopyEnsClick = copied::add,
            )
            actual.addresses.first().onCopyClick()
            actual.onCopyAddressClick()
            actual.onCopyEnsClick()

            // Assert
            assertThat(actual.ensName).isEqualTo(ens.value)
            assertThat(copied).containsExactly(defaultAddress, legacy, ens).inOrder()
        }

        @Test
        fun `GIVEN no ens address WHEN copy ens THEN nothing is copied`() {
            // Arrange
            val factory = createFactory(currency = createCoin(Blockchain.Ethereum))
            val copied = mutableListOf<ReceiveAddress>()

            // Act
            val actual = factory.createScreen(addresses = listOf(defaultAddress), onCopyEnsClick = copied::add)
            actual.onCopyEnsClick()

            // Assert
            assertThat(actual.ensName).isNull()
            assertThat(copied).isEmpty()
        }
    }

    data class CardsModel(val addresses: List<ReceiveAddress>, val expected: List<Card>)

    data class Card(val value: String, val label: TextReference?, val isDynamic: Boolean)

    data class TextsModel(
        val currency: CryptoCurrency,
        val tokenName: String,
        val expected: ScreenTexts,
        val asset: Asset = Asset.Currency,
        val type: TokenReceiveType = TokenReceiveType.Default,
    )

    data class ScreenTexts(
        val title: TextReference,
        val subtitle: TextReference,
        val networkNotice: TextReference,
        val extrasHint: ReceiveExtrasHint,
    )

    private val defaultAddress = ReceiveAddress(
        value = "0x3987734c3379A3E2e3eF21c3C8090Cc7aBCD6260",
        type = ReceiveAddress.Type.Primary.Default(displayName = stringReference("Default")),
    )

    private val ens = ReceiveAddress(value = "cryptomonster.eth", type = ReceiveAddress.Type.Ens)

    private val legacy = ReceiveAddress(
        value = "1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2",
        type = ReceiveAddress.Type.Primary.Legacy(displayName = stringReference("Legacy")),
    )

    private val dynamic = ReceiveAddress(
        value = "bc1qdynamic",
        type = ReceiveAddress.Type.Primary.Dynamic(displayName = stringReference("Dynamic")),
    )

    private fun ReceiveScreenStateFactory.createScreen(
        addresses: List<ReceiveAddress>,
        selectedIndex: Int = 0,
        onCopyAddressClick: (ReceiveAddress) -> Unit = {},
        onCopyEnsClick: (ReceiveAddress) -> Unit = {},
    ): ReceiveScreenUM = create(
        addresses = addresses,
        selectedIndex = selectedIndex,
        onCopyAddressClick = onCopyAddressClick,
        onCopyEnsClick = onCopyEnsClick,
        onAddressPageChange = {},
    )

    private fun ReceiveScreenUM.texts() = ScreenTexts(
        title = title,
        subtitle = subtitle,
        networkNotice = networkNotice,
        extrasHint = extrasHint,
    )

    private fun subtitle(currency: CryptoCurrency, label: String?): TextReference = if (label != null) {
        resourceReference(R.string.receive_network_subtitle, wrappedList(currency.network.name, label))
    } else {
        resourceReference(R.string.domain_receive_assets_onboarding_network_name, wrappedList(currency.network.name))
    }

    private fun notice(tokenName: String, currency: CryptoCurrency, label: String?): TextReference =
        if (label != null) {
            resourceReference(
                id = R.string.receive_send_only_on_network_with_label,
                formatArgs = wrappedList(tokenName, currency.network.name, label),
            )
        } else {
            resourceReference(R.string.receive_send_only_on_network, wrappedList(tokenName, currency.network.name))
        }

    private fun createFactory(
        currency: CryptoCurrency,
        asset: Asset = Asset.Currency,
        type: TokenReceiveType = TokenReceiveType.Default,
        tokenName: String = currency.symbol,
    ) = ReceiveScreenStateFactory(
        cryptoCurrency = currency,
        asset = asset,
        type = type,
        tokenName = tokenName,
        shouldShowMemoDisclaimer = true,
        iconState = CurrencyIconState.Loading,
        warnings = persistentListOf(),
    )

    private fun createCoin(
        blockchain: Blockchain,
        extrasType: Network.TransactionExtrasType = Network.TransactionExtrasType.NONE,
    ): CryptoCurrency.Coin = CryptoCurrency.Coin(
        id = createId(blockchain, CryptoCurrency.ID.Prefix.COIN_PREFIX),
        network = createNetwork(blockchain, Network.StandardType.Unspecified(blockchain.name), extrasType),
        name = blockchain.fullName,
        symbol = blockchain.currency,
        decimals = 8,
        iconUrl = null,
        isCustom = false,
    )

    private fun createToken(blockchain: Blockchain, standardType: Network.StandardType): CryptoCurrency.Token =
        CryptoCurrency.Token(
            id = createId(blockchain, CryptoCurrency.ID.Prefix.TOKEN_PREFIX),
            network = createNetwork(blockchain, standardType, Network.TransactionExtrasType.NONE),
            name = "Tether",
            symbol = "USDT",
            decimals = 6,
            iconUrl = null,
            isCustom = false,
            contractAddress = "0xdac17f958d2ee523a2206206994597c13d831ec7",
        )

    private fun createId(blockchain: Blockchain, prefix: CryptoCurrency.ID.Prefix) = CryptoCurrency.ID(
        prefix = prefix,
        body = CryptoCurrency.ID.Body.NetworkId(blockchain.toNetworkId()),
        suffix = CryptoCurrency.ID.Suffix.RawID(blockchain.id),
    )

    private fun createNetwork(
        blockchain: Blockchain,
        standardType: Network.StandardType,
        extrasType: Network.TransactionExtrasType,
    ) = Network(
        id = Network.ID(value = blockchain.toNetworkId(), derivationPath = Network.DerivationPath.None),
        name = blockchain.fullName,
        currencySymbol = blockchain.currency,
        derivationPath = Network.DerivationPath.None,
        isTestnet = false,
        standardType = standardType,
        hasFiatFeeRate = true,
        canHandleTokens = true,
        transactionExtrasType = extrasType,
        nameResolvingType = Network.NameResolvingType.NONE,
    )
}