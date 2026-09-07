package com.tangem.domain.models.currency

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency.ID.Body
import com.tangem.domain.models.network.Network
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CryptoCurrencyIdBodyTest {

    @ParameterizedTest
    @ProvideTestModels
    fun create(model: CreateModel) {
        // Act
        val actual = Body.create(rawId = RAW_ID, derivationPath = model.derivationPath)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        CreateModel(
            derivationPath = Network.DerivationPath.None,
            expected = Body.NetworkId(rawId = RAW_ID),
        ),
        CreateModel(
            derivationPath = Network.DerivationPath.Card(value = CARD_PATH),
            expected = Body.NetworkIdWithDerivationPath(rawId = RAW_ID, derivationPathHashCode = CARD_PATH.hashCode()),
        ),
        CreateModel(
            derivationPath = Network.DerivationPath.Custom(value = JOINT_PATH),
            expected = Body.NetworkIdWithDerivationPath(rawId = RAW_ID, derivationPathHashCode = JOINT_PATH.hashCode()),
        ),
        CreateModel(
            derivationPath = Network.DerivationPath.Joint(value = JOINT_PATH),
            expected = Body.NetworkIdWithDerivationPath(
                rawId = RAW_ID,
                derivationPathHashCode = "joint:$JOINT_PATH".hashCode(),
            ),
        ),
    )

    @Test
    fun `GIVEN joint and custom paths with the same raw value WHEN create THEN bodies differ`() {
        // Act
        val jointBody = Body.create(rawId = RAW_ID, derivationPath = Network.DerivationPath.Joint(value = JOINT_PATH))
        val customBody = Body.create(rawId = RAW_ID, derivationPath = Network.DerivationPath.Custom(value = JOINT_PATH))

        // Assert
        assertThat(jointBody).isNotEqualTo(customBody)
        assertThat(jointBody.value).isNotEqualTo(customBody.value)
    }

    data class CreateModel(val derivationPath: Network.DerivationPath, val expected: Body)

    @Test
    fun `GIVEN joint currency id WHEN restored from its raw string THEN equal to the original`() {
        // Arrange — deeplinks and pushes rebuild the id from its string form; the salted hash must survive it
        val original = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = Body.create(rawId = RAW_ID, derivationPath = Network.DerivationPath.Joint(value = JOINT_PATH)),
            suffix = CryptoCurrency.ID.Suffix.RawID(rawId = "usdt", contractAddress = CONTRACT_ADDRESS),
        )

        // Act
        val restored = CryptoCurrency.ID.fromValue(value = original.value)

        // Assert
        assertThat(restored).isEqualTo(original)
        assertThat(restored.value).isEqualTo(original.value)
    }

    private companion object {
        const val RAW_ID = "ethereum"
        const val CARD_PATH = "m/44'/60'/0'/0/0"
        const val CONTRACT_ADDRESS = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
        const val JOINT_PATH = "m/44'/60'/888888'/0/1"
    }
}