package com.tangem.features.collectibles.impl.confirmorder.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.test.core.ProvideTestModels
import com.tangem.features.collectibles.impl.confirmorder.ui.state.CollectiblesConfirmOrderUM.ButtonUM
import com.tangem.features.collectibles.impl.createMachine
import com.tangem.test.mock.collectibles.MockCollectibles
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectiblesConfirmOrderConverterTest {

    @Test
    fun `GIVEN a machine WHEN convert THEN the order reads pack fee and total`() {
        // Arrange
        val machine = createMachine(
            shortName = "Elite pack",
            price = BigDecimal("50"),
            feeAmount = BigDecimal("1"),
        )

        // Act
        val actual = convert(input(machine))

        // Assert
        assertThat(actual.rows.map { it.labelRef })
            .containsExactly(
                stringReference("Elite pack x1"),
                resourceReference(R.string.collectibles_confirm_order_platform_fee),
                resourceReference(R.string.collectibles_confirm_order_total),
            )
            .inOrder()
        assertThat(actual.rows.map { it.value })
            .containsExactly("$50", "$1", usdc("51"))
            .inOrder()
    }

    @Test
    fun `GIVEN a price and a fee WHEN convert THEN the total is their sum`() {
        // Arrange
        val machine = createMachine(price = BigDecimal("50"), feeAmount = BigDecimal("2.50"))

        // Act
        val actual = convert(input(machine))

        // Assert
        assertThat(actual.rows.last().value).isEqualTo(usdc("52.5"))
    }

    @Test
    fun `GIVEN the total row WHEN convert THEN it is emphasised and undivided`() {
        // Act
        val actual = convert(input(createMachine()))

        // Assert
        val total = actual.rows.last()
        assertThat(total.isEmphasised).isTrue()
        assertThat(total.hasDivider).isFalse()
    }

    @Test
    fun `GIVEN a blank short name WHEN convert THEN the full name titles the row`() {
        // Arrange
        val machine = createMachine(name = "Pokemon Legendary Collection", shortName = "")

        // Act
        val actual = convert(input(machine))

        // Assert
        assertThat(actual.rows.first().labelRef).isEqualTo(stringReference("Pokemon Legendary Collection x1"))
    }

    @Test
    fun `GIVEN a machine WHEN convert THEN the CTA reads Approve and carries the pack artwork`() {
        // Arrange
        val machine = createMachine(imageUrl = "https://tangem.com/pack.png")

        // Act
        val actual = convert(input(machine))

        // Assert
        assertThat(actual.button).isEqualTo(APPROVE)
        assertThat(actual.packImageUrl).isEqualTo("https://tangem.com/pack.png")
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a vault balance and a confirm state WHEN convert THEN the button reflects them`(model: ButtonModel) {
        // Arrange
        val machine = createMachine(price = BigDecimal("50"), feeAmount = BigDecimal("1"))

        // Act
        val actual = convert(
            input(machine = machine, usdcBalance = model.usdcBalance, isConfirming = model.isConfirming),
        )

        // Assert
        assertThat(actual.button).isEqualTo(model.expected)
    }

    internal data class ButtonModel(
        val usdcBalance: BigDecimal,
        val isConfirming: Boolean = false,
        val expected: ButtonUM,
    )

    private fun provideTestModels() = listOf(
        ButtonModel(usdcBalance = BigDecimal("100"), expected = APPROVE),
        ButtonModel(usdcBalance = BigDecimal("51"), expected = APPROVE),
        ButtonModel(usdcBalance = BigDecimal("50.99"), expected = NOT_ENOUGH_FUNDS),
        ButtonModel(usdcBalance = BigDecimal.ZERO, expected = NOT_ENOUGH_FUNDS),
        ButtonModel(usdcBalance = BigDecimal("100"), isConfirming = true, expected = OPENING),
        ButtonModel(usdcBalance = BigDecimal.ZERO, isConfirming = true, expected = OPENING),
    )

    private val converter = CollectiblesConfirmOrderConverter(onBackClick = {}, onApproveClick = {})

    private fun convert(input: CollectiblesConfirmOrderConverter.Input) = converter.convert(input).order

    private fun input(
        machine: CollectibleGachaMachine,
        usdcBalance: BigDecimal = BigDecimal("1000"),
        isConfirming: Boolean = false,
    ) = CollectiblesConfirmOrderConverter.Input(
        machine = machine,
        vaultBalance = MockCollectibles.vaultBalance(amount = usdcBalance),
        isConfirming = isConfirming,
    )

    private fun usdc(amount: String) = BigDecimal(amount).format { crypto(cryptoCurrency = MockCollectibles.usdc) }

    private companion object {

        val APPROVE = ButtonUM(
            textRef = resourceReference(R.string.collectibles_confirm_order_approve_button),
            isEnabled = true,
        )
        val NOT_ENOUGH_FUNDS = ButtonUM(
            textRef = resourceReference(R.string.collectibles_confirm_order_not_enough_funds_button),
            isEnabled = false,
        )
        val OPENING = ButtonUM(textRef = stringReference("Opening pack…"), isEnabled = false)
    }
}