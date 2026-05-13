package com.tangem.domain.dynamicaddresses

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.derivations.DerivationStyleProvider
import com.tangem.domain.wallets.derivations.derivationStyleProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsDynamicAddressesAvailableUseCaseTest {

    private val featureToggles: DynamicAddressesFeatureToggles = mockk()
    private val useCase = IsDynamicAddressesAvailableUseCase(featureToggles)

    @BeforeAll
    fun setup() {
        mockkStatic("com.tangem.domain.wallets.derivations.DerivationStyleProviderExtKt")
        every { featureToggles.isDynamicAddressesEnabled } returns true
    }

    @AfterAll
    fun teardown() {
        unmockkAll()
    }

    // region Gating

    @Test
    fun `feature toggle off returns false`() {
        every { featureToggles.isDynamicAddressesEnabled } returns false

        val coin = coin(Blockchain.Bitcoin, "m/84'/0'/0'/0/0")
        val result = useCase(walletWithStyle(DerivationStyle.V3), coin)

        assertThat(result).isFalse()
        every { featureToggles.isDynamicAddressesEnabled } returns true // restore
    }

    @Test
    fun `token currency returns false`() {
        val token = token(Blockchain.Ethereum, "m/44'/60'/0'/0/0")
        val result = useCase(walletWithStyle(DerivationStyle.V3), token)
        assertThat(result).isFalse()
    }

    @Test
    fun `unsupported network returns false`() {
        val coin = coin(Blockchain.Ethereum, "m/44'/60'/0'/0/0")
        val result = useCase(walletWithStyle(DerivationStyle.V3), coin)
        assertThat(result).isFalse()
    }

    @Test
    fun `non-HD wallet returns false`() {
        val coin = coin(Blockchain.Bitcoin, "m/84'/0'/0'/0/0")
        val result = useCase(walletWithStyle(style = null), coin)
        assertThat(result).isFalse()
    }

    // endregion

    // region BTC: V2 (Wallet 1) ↔ V3 (Wallet 2 / Hot)

    @Test
    fun `V3 wallet accepts BIP-84 BTC`() {
        val coin = coin(Blockchain.Bitcoin, "m/84'/0'/0'/0/0")
        val result = useCase(walletWithStyle(DerivationStyle.V3), coin)
        assertThat(result).isTrue()
    }

    @Test
    fun `V3 wallet rejects BIP-44 BTC`() {
        val coin = coin(Blockchain.Bitcoin, "m/44'/0'/0'/0/0")
        val result = useCase(walletWithStyle(DerivationStyle.V3), coin)
        assertThat(result).isFalse()
    }

    @Test
    fun `V2 wallet accepts BIP-44 BTC`() {
        val coin = coin(Blockchain.Bitcoin, "m/44'/0'/0'/0/0")
        val result = useCase(walletWithStyle(DerivationStyle.V2), coin)
        assertThat(result).isTrue()
    }

    @Test
    fun `V2 wallet rejects BIP-84 BTC`() {
        val coin = coin(Blockchain.Bitcoin, "m/84'/0'/0'/0/0")
        val result = useCase(walletWithStyle(DerivationStyle.V2), coin)
        assertThat(result).isFalse()
    }

    // endregion

    // region LTC: same dual-style behavior

    @Test
    fun `V3 wallet accepts BIP-84 LTC`() {
        val coin = coin(Blockchain.Litecoin, "m/84'/2'/0'/0/0")
        val result = useCase(walletWithStyle(DerivationStyle.V3), coin)
        assertThat(result).isTrue()
    }

    @Test
    fun `V2 wallet accepts BIP-44 LTC`() {
        val coin = coin(Blockchain.Litecoin, "m/44'/2'/0'/0/0")
        val result = useCase(walletWithStyle(DerivationStyle.V2), coin)
        assertThat(result).isTrue()
    }

    @Test
    fun `coin_type mismatch is rejected`() {
        // BTC coin_type is 0; using LTC's coin_type 2 must fail
        val coin = coin(Blockchain.Bitcoin, "m/84'/2'/0'/0/0")
        val result = useCase(walletWithStyle(DerivationStyle.V3), coin)
        assertThat(result).isFalse()
    }

    // endregion

    // region Other supported chains (V2 and V3 share BIP-44)

    @Test
    fun `V3 wallet accepts BIP-44 Dogecoin`() {
        val coin = coin(Blockchain.Dogecoin, "m/44'/3'/0'/0/0")
        val result = useCase(walletWithStyle(DerivationStyle.V3), coin)
        assertThat(result).isTrue()
    }

    @Test
    fun `V2 wallet accepts BIP-44 Dash`() {
        val coin = coin(Blockchain.Dash, "m/44'/5'/0'/0/0")
        val result = useCase(walletWithStyle(DerivationStyle.V2), coin)
        assertThat(result).isTrue()
    }

    // endregion

    // region Account & non-base path

    @Test
    fun `secondary account is accepted`() {
        val coin = coin(Blockchain.Bitcoin, "m/84'/0'/3'/0/0")
        val result = useCase(walletWithStyle(DerivationStyle.V3), coin)
        assertThat(result).isTrue()
    }

    @Test
    fun `non-zero change is rejected`() {
        val coin = coin(Blockchain.Bitcoin, "m/84'/0'/0'/1/0")
        val result = useCase(walletWithStyle(DerivationStyle.V3), coin)
        assertThat(result).isFalse()
    }

    @Test
    fun `non-zero address index is rejected`() {
        val coin = coin(Blockchain.Bitcoin, "m/84'/0'/0'/0/5")
        val result = useCase(walletWithStyle(DerivationStyle.V3), coin)
        assertThat(result).isFalse()
    }

    @Test
    fun `path with fewer than 5 nodes is rejected`() {
        val coin = coin(Blockchain.Bitcoin, "m/84'/0'/0'")
        val result = useCase(walletWithStyle(DerivationStyle.V3), coin)
        assertThat(result).isFalse()
    }

    // endregion

    // region helpers

    private fun walletWithStyle(style: DerivationStyle?): UserWallet {
        val wallet: UserWallet = mockk()
        val provider = object : DerivationStyleProvider {
            override fun getDerivationStyle(): DerivationStyle? = style
        }
        every { wallet.derivationStyleProvider } returns provider
        return wallet
    }

    private fun network(blockchain: Blockchain, derivationPathValue: String): Network {
        val derivationPath = Network.DerivationPath.Card(derivationPathValue)
        return Network(
            id = Network.ID(value = blockchain.toNetworkId(), derivationPath = derivationPath),
            name = blockchain.fullName,
            currencySymbol = blockchain.currency,
            derivationPath = derivationPath,
            isTestnet = blockchain.isTestnet(),
            standardType = Network.StandardType.Unspecified(blockchain.fullName),
            hasFiatFeeRate = true,
            canHandleTokens = false,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
    }

    private fun coin(blockchain: Blockchain, derivationPathValue: String): CryptoCurrency.Coin {
        val coin: CryptoCurrency.Coin = mockk()
        every { coin.network } returns network(blockchain, derivationPathValue)
        return coin
    }

    private fun token(blockchain: Blockchain, derivationPathValue: String): CryptoCurrency.Token {
        val token: CryptoCurrency.Token = mockk()
        every { token.network } returns network(blockchain, derivationPathValue)
        return token
    }

    // endregion
}