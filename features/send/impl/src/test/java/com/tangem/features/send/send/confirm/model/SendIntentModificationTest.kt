package com.tangem.features.send.send.confirm.model

import com.google.common.truth.Truth.assertThat
import com.tangem.features.send.send.confirm.SendConfirmComponent
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.Resources
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@ResourceLock(Resources.LOCALE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SendIntentModificationTest {

    private val intent = SendConfirmComponent.Intent(
        id = "intent-1",
        amount = "1",
        destinationAddress = EVM_ADDRESS,
        memo = null,
    )

    private val asAsked = SentTransfer(
        amount = BigDecimal.ONE,
        recipient = EVM_ADDRESS,
        memo = null,
        decimals = 6,
        networkRawId = "ethereum",
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN sent transfer WHEN compared with the intent THEN modification is told`(model: ModificationModel) {
        // Act
        val actual = model.intent.isModifiedBy(model.sent)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class ModificationModel(
        val description: String,
        val intent: SendConfirmComponent.Intent,
        val sent: SentTransfer,
        val expected: Boolean,
    ) {
        override fun toString() = description
    }

    private fun provideTestModels() = listOf(
        ModificationModel("sent as asked", intent, asAsked, expected = false),
        ModificationModel(
            description = "intent amount with trailing zeros",
            intent = intent.copy(amount = "1.000"),
            sent = asAsked,
            expected = false,
        ),
        ModificationModel("amount changed", intent, asAsked.copy(amount = BigDecimal("2")), expected = true),
        ModificationModel("fee taken out", intent, asAsked.copy(amount = BigDecimal("0.99")), expected = true),
        ModificationModel("amount unknown", intent, asAsked.copy(amount = null), expected = true),
        ModificationModel("recipient changed", intent, asAsked.copy(recipient = OTHER_EVM_ADDRESS), expected = true),
        ModificationModel(
            description = "EVM recipient in another case",
            intent = intent,
            sent = asAsked.copy(recipient = EVM_ADDRESS.lowercase()),
            expected = false,
        ),
        ModificationModel(
            description = "Solana recipient in another case",
            intent = intent.copy(destinationAddress = SOLANA_ADDRESS),
            sent = asAsked.copy(recipient = SOLANA_ADDRESS.lowercase(), networkRawId = "solana"),
            expected = true,
        ),
        ModificationModel("memo added", intent, asAsked.copy(memo = "memo-7"), expected = true),
        ModificationModel("empty memo for none", intent, asAsked.copy(memo = ""), expected = false),
    )

    private companion object {
        const val EVM_ADDRESS = "0x52908400098527886E0F7030069857D2E4169EE7"
        const val OTHER_EVM_ADDRESS = "0x8617E340B3D01FA5F11F306F4090FD50E238070D"
        const val SOLANA_ADDRESS = "9WzDXwBbmkg8ZTbNMqUxvQRAyrZzDsGYdLVL9zYtAWWM"
    }
}