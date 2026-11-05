package com.tangem.features.earn.product.api.product

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.modal.TangemModalContentUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.earn.api.ui.EarnRowUM
import org.junit.jupiter.api.Test

internal class EarnRowMappingTest {

    @Test
    fun `GIVEN row with info WHEN toUM and info clicked THEN callback receives that text block`() {
        // Arrange
        val info = createTextBlock(title = "APY", description = "Annual percentage yield")
        val row = createRow(info = info)
        val received = mutableListOf<EarnTextBlock>()

        // Act
        row.toUM(onInfoClick = received::add).onInfoClick?.invoke()

        // Assert
        assertThat(received).containsExactly(info)
    }

    @Test
    fun `GIVEN row with info WHEN toUM without click THEN callback is not invoked`() {
        // Arrange
        val row = createRow(info = createTextBlock())
        val received = mutableListOf<EarnTextBlock>()

        // Act
        row.toUM(onInfoClick = received::add)

        // Assert
        assertThat(received).isEmpty()
    }

    @Test
    fun `GIVEN row with info WHEN toUM and info clicked twice THEN callback is invoked each time`() {
        // Arrange
        val info = createTextBlock()
        val row = createRow(info = info)
        val received = mutableListOf<EarnTextBlock>()

        // Act
        val onInfoClick = row.toUM(onInfoClick = received::add).onInfoClick
        onInfoClick?.invoke()
        onInfoClick?.invoke()

        // Assert
        assertThat(received).containsExactly(info, info)
    }

    @Test
    fun `GIVEN row without info WHEN toUM THEN onInfoClick is null and texts are carried over`() {
        // Arrange
        val label = stringReference("Rewards")
        val value = stringReference("0.5 ETH")
        val row = createRow(label = label, value = EarnValue.Text(value), info = null)

        // Act
        val actual = row.toUM(onInfoClick = {})

        // Assert
        assertThat(actual).isEqualTo(EarnRowUM(label = label, value = value, onInfoClick = null))
    }

    @Test
    fun `GIVEN row with info WHEN toUM THEN label and value are carried over and onInfoClick is set`() {
        // Arrange
        val label = stringReference("Rewards")
        val value = stringReference("0.5 ETH")
        val row = createRow(label = label, value = EarnValue.Text(value), info = createTextBlock())

        // Act
        val actual = row.toUM(onInfoClick = {})

        // Assert
        val expected = EarnRowUM(label = label, value = value, onInfoClick = null)
        assertThat(actual.onInfoClick).isNotNull()
        assertThat(actual.copy(onInfoClick = null)).isEqualTo(expected)
    }

    @Test
    fun `GIVEN text block WHEN toUM THEN informer item has the same title and description`() {
        // Arrange
        val block = createTextBlock(title = "Withdrawal", description = "Takes up to 3 days")

        // Act
        val actual = block.toUM()

        // Assert
        val expected = TangemModalContentUM.Informer.Item(
            title = stringReference("Withdrawal"),
            description = stringReference("Takes up to 3 days"),
        )
        assertThat(actual).isEqualTo(expected)
    }

    private fun createRow(
        label: TextReference = stringReference("Label"),
        value: EarnValue = EarnValue.Text(stringReference("Value")),
        info: EarnTextBlock? = null,
    ) = EarnRow(label = label, value = value, info = info)

    private fun createTextBlock(title: String = "Title", description: String = "Description") = EarnTextBlock(
        title = stringReference(title),
        description = stringReference(description),
    )
}