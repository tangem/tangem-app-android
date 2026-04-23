package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.warnings.DynamicAddressesWarnings
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class TokenDetailsNotificationConverterTest {

    private val clickIntents: TokenDetailsClickIntents = mockk(relaxed = true)
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk(relaxed = true)
    private val userWalletId: UserWalletId = mockk(relaxed = true)

    private val converter = TokenDetailsNotificationConverter(
        userWalletId = userWalletId,
        getUserWalletUseCase = getUserWalletUseCase,
        clickIntents = clickIntents,
    )

    @Test
    fun `GIVEN FundsFound warning WHEN convert THEN DynamicAddressesFundsFound notification is produced`() {
        // WHEN
        val result = converter.convert(setOf(DynamicAddressesWarnings.FundsFound))

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result.first()).isInstanceOf(TokenDetailsNotification.DynamicAddressesFundsFound::class.java)
    }

    @Test
    fun `GIVEN FundsFound warning WHEN learn more button clicked THEN click intent is invoked`() {
        // GIVEN
        val notification = converter.convert(setOf(DynamicAddressesWarnings.FundsFound)).first()
        val button = notification.config.buttonsState as NotificationConfig.ButtonsState.SecondaryButtonConfig

        // WHEN
        button.onClick()

        // THEN
        verify(exactly = 1) { clickIntents.onDynamicAddressesFundsFoundLearnMoreClick() }
    }
}