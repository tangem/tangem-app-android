package com.tangem.features.earn.product.api.product

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.modal.TangemModalContentUM
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_16
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
        row.toUM(onInfoClick = received::add, onLinkClick = {}).onInfoClick?.invoke()

        // Assert
        assertThat(received).containsExactly(info)
    }

    @Test
    fun `GIVEN row with info WHEN toUM without click THEN callback is not invoked`() {
        // Arrange
        val row = createRow(info = createTextBlock())
        val received = mutableListOf<EarnTextBlock>()

        // Act
        row.toUM(onInfoClick = received::add, onLinkClick = {})

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
        val onInfoClick = row.toUM(onInfoClick = received::add, onLinkClick = {}).onInfoClick
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
        val actual = row.toUM(onInfoClick = {}, onLinkClick = {})

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
        val actual = row.toUM(onInfoClick = {}, onLinkClick = {})

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

    @Test
    fun `GIVEN row with link value WHEN toUM and value clicked THEN callback receives the url`() {
        // Arrange
        val row = createRow(value = EarnValue.Link(text = stringReference("Aave"), url = "https://aave.com"))
        val opened = mutableListOf<String>()

        // Act
        val actual = row.toUM(onInfoClick = {}, onLinkClick = opened::add)
        actual.onValueClick?.invoke()

        // Assert
        assertThat(actual.value).isEqualTo(stringReference("Aave"))
        assertThat(opened).containsExactly("https://aave.com")
    }

    @Test
    fun `GIVEN row with link value WHEN toUM without click THEN the url is not opened`() {
        // Arrange
        val row = createRow(value = EarnValue.Link(text = stringReference("Aave"), url = "https://aave.com"))
        val opened = mutableListOf<String>()

        // Act
        row.toUM(onInfoClick = {}, onLinkClick = opened::add)

        // Assert
        assertThat(opened).isEmpty()
    }

    @Test
    fun `GIVEN row with link value WHEN toUM without link handler THEN value is not clickable`() {
        // Arrange
        val row = createRow(value = EarnValue.Link(text = stringReference("Aave"), url = "https://aave.com"))

        // Act
        val actual = row.toUM(onInfoClick = {})

        // Assert
        assertThat(actual.onValueClick).isNull()
        assertThat(actual.value).isEqualTo(stringReference("Aave"))
    }

    @Test
    fun `GIVEN row with text value WHEN toUM THEN value is not clickable`() {
        // Arrange
        val row = createRow(value = EarnValue.Text(stringReference("Low")))

        // Act
        val actual = row.toUM(onInfoClick = {}, onLinkClick = {})

        // Assert
        assertThat(actual.onValueClick).isNull()
    }

    @Test
    fun `GIVEN text block with icon WHEN toUM THEN informer item carries only title and description`() {
        // Arrange
        val block = EarnTextBlock(
            title = stringReference("No lock-ups"),
            description = stringReference("Withdraw anytime"),
            icon = TangemIconUM.Icon(Icons.ic_info_16),
        )

        // Act
        val actual = block.toUM()

        // Assert
        val expected = TangemModalContentUM.Informer.Item(
            title = stringReference("No lock-ups"),
            description = stringReference("Withdraw anytime"),
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