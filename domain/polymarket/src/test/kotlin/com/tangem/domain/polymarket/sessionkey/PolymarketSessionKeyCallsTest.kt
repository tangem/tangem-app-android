package com.tangem.domain.polymarket.sessionkey

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketSessionKeyCallsTest {

    @Test
    fun `GIVEN a key and its expiry WHEN authorize THEN the deposit wallet is called with both words`() {
        // Act
        val actual = PolymarketSessionKeyCalls.authorize(
            sessionKeyAddress = SESSION_KEY,
            validUntil = 1_804_982_400L,
            depositWalletAddress = DEPOSIT_WALLET,
        )

        // Assert
        val expected = PolymarketApprovalCall(
            target = DEPOSIT_WALLET.lowercase(),
            value = "0",
            data = "0x24017fae" +
                "0000000000000000000000005e55000000000000000000000000000000000001" +
                "000000000000000000000000000000000000000000000000000000006b95d880",
        )
        assertThat(actual).isEqualTo(expected.right())
    }

    @Test
    fun `GIVEN a key WHEN revoke THEN the deposit wallet is called with the key alone`() {
        // Act
        val actual = PolymarketSessionKeyCalls.revoke(
            sessionKeyAddress = SESSION_KEY,
            depositWalletAddress = DEPOSIT_WALLET,
        )

        // Assert
        val expected = PolymarketApprovalCall(
            target = DEPOSIT_WALLET.lowercase(),
            value = "0",
            data = "0xe63f952f" + "0000000000000000000000005e55000000000000000000000000000000000001",
        )
        assertThat(actual).isEqualTo(expected.right())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a malformed address WHEN encoded THEN refused instead of encoded`(model: MalformedModel) {
        // Act
        val revoke = PolymarketSessionKeyCalls.revoke(
            sessionKeyAddress = model.sessionKey,
            depositWalletAddress = model.depositWallet,
        )
        val authorize = PolymarketSessionKeyCalls.authorize(
            sessionKeyAddress = model.sessionKey,
            validUntil = 1_804_982_400L,
            depositWalletAddress = model.depositWallet,
        )

        // Assert
        val expected = PolymarketSessionKeyError.InvalidAddress(model.malformed)
        assertThat(revoke.leftOrNull()).isEqualTo(expected)
        assertThat(authorize.leftOrNull()).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        MalformedModel(sessionKey = "0x1234", depositWallet = DEPOSIT_WALLET, malformed = "0x1234"),
        MalformedModel(sessionKey = NON_HEX, depositWallet = DEPOSIT_WALLET, malformed = NON_HEX),
        MalformedModel(sessionKey = SESSION_KEY, depositWallet = TOO_LONG, malformed = TOO_LONG),
    )

    @Test
    fun `GIVEN a moment with a fraction of a second WHEN validUntil THEN 180 days from its whole second`() {
        // Act
        val actual = PolymarketSessionKeyCalls.validUntil(nowMillis = 1_789_430_400_750L)

        // Assert
        assertThat(actual).isEqualTo(1_789_430_400L + 15_552_000L)
    }

    data class MalformedModel(val sessionKey: String, val depositWallet: String, val malformed: String)

    private companion object {
        const val SESSION_KEY = "0x5E55000000000000000000000000000000000001"
        const val DEPOSIT_WALLET = "0xfAeA0f08159fcF2f573fE24E9E989B0d48f7651B"
        val NON_HEX = "0x" + "z".repeat(40)
        val TOO_LONG = "0x" + "a".repeat(41)
    }
}