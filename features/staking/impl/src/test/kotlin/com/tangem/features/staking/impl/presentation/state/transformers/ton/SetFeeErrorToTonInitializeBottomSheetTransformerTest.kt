package com.tangem.features.staking.impl.presentation.state.transformers.ton

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.bottomsheet.TonInitializeAccountBottomSheetConfig
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import org.junit.jupiter.api.Test

internal class SetFeeErrorToTonInitializeBottomSheetTransformerTest {

    private val transformer = SetFeeErrorToTonInitializeBottomSheetTransformer()

    @Test
    fun `GIVEN activation sheet WHEN fee failed THEN sheet shows fee error and disables button`() {
        // Arrange
        val content = TonInitializeAccountBottomSheetConfig(
            title = TextReference.EMPTY,
            message = TextReference.EMPTY,
            footer = TextReference.EMPTY,
            onButtonClick = {},
            isButtonEnabled = true,
            isButtonLoading = true,
            feeState = FeeState.Loading,
            isColdWalletInteractionIconVisible = true,
        )
        val sheet = TangemBottomSheetConfig(isShown = true, onDismissRequest = {}, content = content)
        val state = stakingUiState(bottomSheetConfig = sheet)

        // Act
        val actual = transformer.transform(state)

        // Assert
        assertThat(actual).isEqualTo(
            state.copy(
                bottomSheetConfig = sheet.copy(
                    content = content.copy(isButtonEnabled = false, feeState = FeeState.Error, isButtonLoading = false),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN no bottom sheet WHEN fee failed THEN same state`() {
        // Arrange
        val state = stakingUiState()

        // Act
        val actual = transformer.transform(state)

        // Assert
        assertThat(actual).isSameInstanceAs(state)
    }
}