package com.tangem.tap.domain.card

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.cardano.CardanoUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.derivation.DerivationConfigV2
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.common.configs.GenericCardConfig
import com.tangem.domain.common.configs.MultiWalletCardConfig
import com.tangem.domain.common.configs.Wallet2CardConfig
import com.tangem.domain.common.util.derivationStyleProvider
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class MissedDerivationsFinderTest {

    @Test
    fun `empty derivations for empty currencies`() {
        val scanResponse = MockScanResponseFactory.create(cardConfig = GenericCardConfig(2), derivedKeys = emptyMap())
        val finder = MissedDerivationsFinder(scanResponse)

        val actual = finder.find(emptyList())

        Truth.assertThat(actual).isEmpty()
    }

    @Test
    fun `empty derivations for non supported blockchains`() {
        // Bls is not supported
        val scanResponse = MockScanResponseFactory.create(cardConfig = GenericCardConfig(2), derivedKeys = emptyMap())
        val finder = MissedDerivationsFinder(scanResponse)

        val currencies = MockCryptoCurrencyFactory(scanResponse).chia.let(::listOf)
        val actual = finder.find(currencies)

        Truth.assertThat(actual).isEmpty()
    }

    @Test
    fun `derivations ONLY for supported blockchains`() {
        // Bls is not supported
        val scanResponse = MockScanResponseFactory.create(
            cardConfig = GenericCardConfig(2),
            derivedKeys = emptyMap(),
        ).let {
            it.copy(
                card = it.card.copy(
                    settings = it.card.settings.copy(isHDWalletAllowed = true, isBackupAllowed = true),
                ),
            )
        }
        val finder = MissedDerivationsFinder(scanResponse)

        val currencies = MockCryptoCurrencyFactory(scanResponse).chiaAndEthereum
        val actual = finder.find(currencies)

        Truth.assertThat(actual).containsExactly(
            ByteArrayKey(EllipticCurve.Secp256k1.name.toByteArray()),
            listOf(DerivationConfigV2.derivations(Blockchain.Ethereum).values.first()),
        )
    }

    @Test
    fun `derivations for custom token`() {
        val scanResponse = MockScanResponseFactory.create(cardConfig = MultiWalletCardConfig, derivedKeys = emptyMap())
        val finder = MissedDerivationsFinder(scanResponse)

        val currencies = MockCryptoCurrencyFactory(scanResponse).ethereumTokenWithBinanceDerivation
        val actual = finder.find(currencies)

        Truth.assertThat(actual).containsExactly(
            ByteArrayKey(EllipticCurve.Secp256k1.name.toByteArray()),
            listOf(
                DerivationConfigV2.derivations(Blockchain.Ethereum).values.first(),
                DerivationConfigV2.derivations(Blockchain.Binance).values.first(),
            ),
        )
    }

    @Test
    fun `derivations for cardano`() {
        val scanResponse = MockScanResponseFactory.create(cardConfig = MultiWalletCardConfig, derivedKeys = emptyMap())
        val finder = MissedDerivationsFinder(scanResponse)

        val currencies = MockCryptoCurrencyFactory(scanResponse).cardano.let(::listOf)
        val actual = finder.find(currencies)

        Truth.assertThat(actual).containsExactly(
            ByteArrayKey(EllipticCurve.Ed25519.name.toByteArray()),
            listOf(
                DerivationConfigV2.derivations(Blockchain.Cardano).values.first(),
                CardanoUtils.extendedDerivationPath(
                    derivationPath = DerivationPath(
                        Blockchain.Cardano.derivationPath(scanResponse.derivationStyleProvider.getDerivationStyle())!!
                            .rawPath,
                    ),
                ),
            ),
        )
    }

    @Test
    fun `empty derivations for already derived currencies`() {
        val scanResponse = MockScanResponseFactory.create(
            cardConfig = Wallet2CardConfig,
            derivedKeys = DerivedKeysMocks.ethereumDerivedKeys,
        )
        val finder = MissedDerivationsFinder(scanResponse)

        val currencies = MockCryptoCurrencyFactory(scanResponse).ethereum.let(::listOf)
        val actual = finder.find(currencies)

        Truth.assertThat(actual).isEmpty()
    }

    @Test
    fun `derivations ONLY for never derived currencies`() {
        val scanResponse = MockScanResponseFactory.create(
            cardConfig = MultiWalletCardConfig,
            derivedKeys = DerivedKeysMocks.ethereumDerivedKeys,
        )
        val finder = MissedDerivationsFinder(scanResponse)

        val currencies = MockCryptoCurrencyFactory(scanResponse).ethereumAndStellar
        val actual = finder.find(currencies)

        Truth.assertThat(actual).containsExactly(
            ByteArrayKey(EllipticCurve.Ed25519.name.toByteArray()),
            listOf(DerivationConfigV2.derivations(Blockchain.Stellar).values.first()),
        )
    }
}