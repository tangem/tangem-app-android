package com.tangem.features.earn.impl.info

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.earn.product.api.product.EarnActionItem
import com.tangem.features.earn.product.api.product.EarnBlock
import com.tangem.features.earn.product.api.session.EarnAction
import com.tangem.features.earn.product.api.session.Withdraw
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnInfoActionsTest {

    private val actions = EarnInfoActions()
    private val onActionClick: (EarnAction) -> Unit = mockk(relaxed = true)
    private val onBlocked: (EarnBlock) -> Unit = mockk(relaxed = true)

    private val action = Withdraw()
    private val block = EarnBlock(reason = stringReference("blocked"))

    @BeforeEach
    fun resetMocks() {
        clearMocks(onActionClick, onBlocked)
    }

    @Test
    fun `GIVEN item with block WHEN button THEN dimmed and click reports the block, action not invoked`() {
        // Arrange
        val item = EarnActionItem(action = action, block = block, label = stringReference("Withdraw ETH"))

        // Act
        val actual = button(item)
        actual.onClick()

        // Assert
        assertThat(actual.isDimmed).isTrue()
        assertThat(actual.text).isEqualTo(item.label)
        verify(exactly = 1) { onBlocked(block) }
        verify(exactly = 0) { onActionClick(any()) }
    }

    @Test
    fun `GIVEN item without block WHEN button THEN click invokes the action with item action`() {
        // Arrange
        val item = EarnActionItem(action = action, label = stringReference("Withdraw ETH"))

        // Act
        val actual = button(item)
        actual.onClick()

        // Assert
        assertThat(actual.isDimmed).isFalse()
        assertThat(actual.text).isEqualTo(item.label)
        verify(exactly = 1) { onActionClick(action) }
        verify(exactly = 0) { onBlocked(any()) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a button type WHEN button THEN the button has that type`(model: TypeModel) {
        // Act
        val actual = button(EarnActionItem(action = action), type = model.type)

        // Assert
        assertThat(actual.type).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        TypeModel(type = TangemButtonType.Primary, expected = TangemButtonType.Primary),
        TypeModel(type = TangemButtonType.Secondary, expected = TangemButtonType.Secondary),
    )

    internal data class TypeModel(val type: TangemButtonType, val expected: TangemButtonType)

    private fun button(item: EarnActionItem, type: TangemButtonType = TangemButtonType.Primary) = actions.button(
        item = item,
        type = type,
        onActionClick = onActionClick,
        onBlocked = onBlocked,
    )
}