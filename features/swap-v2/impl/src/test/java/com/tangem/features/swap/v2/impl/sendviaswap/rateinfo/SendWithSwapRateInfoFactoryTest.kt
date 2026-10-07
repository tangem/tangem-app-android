package com.tangem.features.swap.v2.impl.sendviaswap.rateinfo

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.features.swap.v2.impl.R
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SendWithSwapRateInfoFactoryTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN rate type WHEN getRateTypeMessage THEN matching info block and got it button built`(
        model: RateTypeModel,
    ) {
        // Act
        val actual = SendWithSwapRateInfoFactory.getRateTypeMessage(expressRateType = model.rateType, onDismiss = {})

        // Assert
        val actualButton = actual.elements.filterIsInstance<MessageBottomSheetUM.Button>().single()
        val expectedButton = MessageBottomSheetUM.Button(
            text = resourceReference(R.string.common_got_it),
            onClick = actualButton.onClick,
        )
        assertThat(actual.elements).containsExactly(model.expectedInfoBlock, expectedButton).inOrder()
    }

    @Test
    fun `GIVEN onDismiss callback WHEN dismiss requested THEN callback invoked`() {
        // Arrange
        var dismissCount = 0
        val message = SendWithSwapRateInfoFactory.getRateTypeMessage(ExpressRateType.Fixed) { dismissCount++ }

        // Act
        message.onDismissRequest()

        // Assert
        assertThat(dismissCount).isEqualTo(1)
    }

    @Test
    fun `GIVEN onDismiss callback WHEN got it button clicked THEN callback invoked`() {
        // Arrange
        var dismissCount = 0
        val message = SendWithSwapRateInfoFactory.getRateTypeMessage(ExpressRateType.Float) { dismissCount++ }
        val button = message.elements.filterIsInstance<MessageBottomSheetUM.Button>().single()

        // Act
        button.onClick?.invoke(message.closeScope)

        // Assert
        assertThat(dismissCount).isEqualTo(1)
    }

    private fun provideTestModels(): List<RateTypeModel> = listOf(
        RateTypeModel(
            rateType = ExpressRateType.Fixed,
            expectedInfoBlock = MessageBottomSheetUM.InfoBlock(
                iconImage = MessageBottomSheetUM.IconImage(R.drawable.ic_fixed_32),
                title = resourceReference(R.string.send_rate_fixed_info_title),
                body = resourceReference(R.string.send_rate_fixed_info_description),
            ),
        ),
        RateTypeModel(
            rateType = ExpressRateType.Float,
            expectedInfoBlock = MessageBottomSheetUM.InfoBlock(
                iconImage = MessageBottomSheetUM.IconImage(R.drawable.ic_floating_32),
                title = resourceReference(R.string.send_rate_floating_info_title),
                body = resourceReference(R.string.send_rate_floating_info_description),
            ),
        ),
    )

    data class RateTypeModel(
        val rateType: ExpressRateType,
        val expectedInfoBlock: MessageBottomSheetUM.InfoBlock,
    )
}