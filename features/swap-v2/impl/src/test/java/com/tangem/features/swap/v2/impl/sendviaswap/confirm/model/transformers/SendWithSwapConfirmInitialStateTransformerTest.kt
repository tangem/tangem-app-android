package com.tangem.features.swap.v2.impl.sendviaswap.confirm.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SendWithSwapConfirmInitialStateTransformerTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN any prevState WHEN transform THEN returns fresh initial content ignoring prevState`(
        model: TransformModel,
    ) {
        // Arrange
        val transformer = SendWithSwapConfirmInitialStateTransformer(isShowTapHelp = model.isShowTapHelp)
        val expected = ConfirmUM.Content(
            isPrimaryButtonEnabled = false,
            isTransactionInProcess = false,
            isShowTapHelp = model.isShowTapHelp,
            sendingFooter = TextReference.EMPTY,
            notifications = persistentListOf(),
            tosUM = null,
        )

        // Act
        val actual = transformer.transform(model.prevState)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(true, false).flatMap { isShowTapHelp ->
        listOf(ConfirmUM.Empty, buildPopulatedContent()).map { prevState ->
            TransformModel(prevState = prevState, isShowTapHelp = isShowTapHelp)
        }
    }

    private fun buildPopulatedContent() = ConfirmUM.Content(
        isPrimaryButtonEnabled = true,
        isTransactionInProcess = true,
        isShowTapHelp = true,
        sendingFooter = stringReference("previous footer"),
        notifications = persistentListOf(NotificationUM.Warning.HighNetworkFee),
        tosUM = ConfirmUM.Content.TosUM(
            tosLink = ConfirmUM.Content.LegalUM(title = stringReference("Terms"), link = "https://terms"),
            policyLink = null,
        ),
    )

    data class TransformModel(val prevState: ConfirmUM, val isShowTapHelp: Boolean)
}