package com.tangem.features.tangempay.cashback.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.pay.model.CashbackPromotions
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayAdditionalCashbackUM
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.joda.time.DateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

private const val FORMATTED_DATE = "26.09.2026"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayAdditionalCashbackConverterTest {

    private val dateFormatter: TangemPayCashbackDateFormatter = mockk()
    private val converter = TangemPayAdditionalCashbackConverter(dateFormatter)

    @BeforeEach
    fun setup() {
        clearMocks(dateFormatter)
        every { dateFormatter.formatNumericDate(any()) } returns FORMATTED_DATE
    }

    @Test
    fun `GIVEN empty list WHEN convert THEN no items`() {
        // Act
        val result = converter.convert(emptyList())

        // Assert
        assertThat(result.items).isEmpty()
    }

    @Test
    fun `GIVEN promo WHEN convert THEN item fields mapped`() {
        // Arrange
        val promo = additional(id = "1", name = "Groceries increase", description = "+1%", isPermanent = true)

        // Act
        val result = converter.convert(listOf(promo))

        // Assert
        assertThat(result.items).containsExactly(
            TangemPayAdditionalCashbackUM.Item(
                id = "1",
                name = stringReference("Groceries increase"),
                description = stringReference("+1%"),
                badge = TangemPayAdditionalCashbackUM.Badge.Permanent,
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("badgeCases")
    fun `GIVEN permanence and end date WHEN convert THEN badge reflects them`(model: BadgeCase) {
        // Act
        val result = converter.convert(listOf(additional(isPermanent = model.isPermanent, endDate = model.endDate)))

        // Assert
        assertThat(result.items.single().badge).isEqualTo(model.expected)
    }

    private fun badgeCases() = listOf(
        BadgeCase(isPermanent = true, endDate = null, expected = TangemPayAdditionalCashbackUM.Badge.Permanent),
        BadgeCase(isPermanent = true, endDate = DATE, expected = TangemPayAdditionalCashbackUM.Badge.Permanent),
        BadgeCase(isPermanent = false, endDate = null, expected = TangemPayAdditionalCashbackUM.Badge.Permanent),
        BadgeCase(
            isPermanent = false,
            endDate = DATE,
            expected = TangemPayAdditionalCashbackUM.Badge.Until(
                resourceReference(R.string.tangempay_cashback_additional_until, wrappedList(FORMATTED_DATE)),
            ),
        ),
    )

    internal data class BadgeCase(
        val isPermanent: Boolean,
        val endDate: DateTime?,
        val expected: TangemPayAdditionalCashbackUM.Badge,
    )

    private fun additional(
        id: String = "id",
        name: String = "name",
        description: String = "description",
        isPermanent: Boolean = false,
        endDate: DateTime? = null,
    ) = CashbackPromotions.AdditionalCashback(
        id = id,
        name = name,
        description = description,
        isPermanent = isPermanent,
        endDate = endDate,
    )

    private companion object {
        val DATE: DateTime = DateTime.parse("2026-09-26")
    }
}