package com.tangem.features.staking.impl.presentation.state

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.navigationButtons.NavigationButtonsState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.staking.impl.R
import com.tangem.utils.transformer.Transformer
import io.mockk.mockk
import org.junit.jupiter.api.Test
import com.tangem.core.ui.R as CoreR

internal class StakingStateControllerTest {

    private val hotWallet = UserWallet.Hot(
        name = "Hot",
        walletId = UserWalletId("00aa"),
        hotWalletId = mockk(relaxed = true),
        wallets = null,
        isBackedUp = false,
    )

    @Test
    fun `GIVEN cold wallet WHEN initializeWithUserWallet THEN icon visible and no hold to confirm`() {
        // Arrange
        val controller = createController()

        // Act
        controller.initializeWithUserWallet(MockUserWalletFactory.create())

        // Assert
        assertThat(controller.value.isColdWalletInteractionIconVisible).isTrue()
        assertThat(controller.value.shouldShowHoldToConfirmButton).isFalse()
        assertThat(controller.value.title).isEqualTo(TextReference.EMPTY)
    }

    @Test
    fun `GIVEN hot wallet WHEN initializeWithUserWallet THEN icon hidden and hold to confirm`() {
        // Arrange
        val controller = createController()

        // Act
        controller.initializeWithUserWallet(hotWallet)

        // Assert
        assertThat(controller.value.isColdWalletInteractionIconVisible).isFalse()
        assertThat(controller.value.shouldShowHoldToConfirmButton).isTrue()
    }

    @Test
    fun `GIVEN function update WHEN update THEN buttons and title derived from new state`() {
        // Arrange
        val controller = createController()

        // Act
        controller.update { it.copy(currentStep = StakingStep.Amount) }

        // Assert
        assertThat(controller.value.title).isEqualTo(resourceReference(R.string.send_amount_label))
        assertThat(primaryText(controller)).isEqualTo(resourceReference(CoreR.string.common_next))
    }

    @Test
    fun `GIVEN transformer update WHEN update THEN transformer applied and title derived`() {
        // Arrange
        val controller = createController()

        // Act
        controller.update(transformer { it.copy(currentStep = StakingStep.Validators) })

        // Assert
        assertThat(controller.value.currentStep).isEqualTo(StakingStep.Validators)
        assertThat(controller.value.title).isEqualTo(resourceReference(R.string.staking_validators))
    }

    @Test
    fun `GIVEN two transformers WHEN updateAll THEN applied in order`() {
        // Arrange
        val controller = createController()
        val first = transformer {
            it.copy(currentStep = StakingStep.Amount, cryptoCurrencyName = "A")
        }
        val second = transformer { it.copy(currentStep = StakingStep.Success) }

        // Act
        controller.updateAll(first, second)

        // Assert
        assertThat(controller.value.currentStep).isEqualTo(StakingStep.Success)
        assertThat(controller.value.cryptoCurrencyName).isEqualTo("A")
        assertThat(controller.value.title).isEqualTo(TextReference.EMPTY)
    }

    // Pins current behaviour: clear() also resets the wallet flags set by initializeWithUserWallet.
    @Test
    fun `GIVEN hot wallet and changed state WHEN clear THEN initial state with derived title and wallet flags reset`() {
        // Arrange
        val controller = createController()
        controller.initializeWithUserWallet(hotWallet)
        controller.update { it.copy(currentStep = StakingStep.Amount, cryptoCurrencyName = "X") }

        // Act
        controller.clear()

        // Assert
        val actual = controller.value
        assertThat(actual.currentStep).isEqualTo(StakingStep.InitialInfo)
        assertThat(actual.cryptoCurrencyName).isEmpty()
        assertThat(actual.title).isEqualTo(resourceReference(R.string.staking_title_stake, wrappedList("")))
        assertThat(primaryText(controller)).isEqualTo(resourceReference(CoreR.string.common_stake))
        assertThat(actual.shouldShowHoldToConfirmButton).isFalse()
        assertThat(actual.isColdWalletInteractionIconVisible).isTrue()
    }

    private fun transformer(transform: (StakingUiState) -> StakingUiState) = object : Transformer<StakingUiState> {
        override fun transform(prevState: StakingUiState) = transform(prevState)
    }

    private fun createController() = StakingStateController(urlOpener = mockk(relaxed = true))

    private fun primaryText(controller: StakingStateController) =
        (controller.value.buttonsState as NavigationButtonsState.Data).primaryButton?.textReference
}