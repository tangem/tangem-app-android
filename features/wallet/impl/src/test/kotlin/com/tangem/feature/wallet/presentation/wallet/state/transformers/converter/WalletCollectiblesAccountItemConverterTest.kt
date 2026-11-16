package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.accountrow.TangemAccountRow
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.account.CollectiblesVaultBalance
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.impl.R
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.collectibles.MockCollectibles
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletCollectiblesAccountItemConverterTest {

    private val onAccountClick: (UserWalletId) -> Unit = mockk(relaxed = true)

    private val converter = WalletCollectiblesAccountItemConverter(
        appCurrency = AppCurrency.Default,
        onAccountClick = onAccountClick,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(onAccountClick)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a collectibles status WHEN convert THEN the row state matches it`(model: StateModel) {
        // Act
        val actual = converter.convert(value = model.status.toAccountStatus())

        // Assert
        assertThat(actual?.state).isInstanceOf(model.expected)
        assertThat(actual?.state?.id).isEqualTo(accountId)
    }

    @Test
    fun `GIVEN any shown status WHEN convert THEN the avatar is the gacha account one`() {
        // Act
        val actual = converter.convert(value = onboarded().toAccountStatus())

        // Assert
        assertThat(actual?.avatar).isEqualTo(TangemAvatar.UiState.Account(kind = TangemAvatar.AccountKind.Gacha))
    }

    @Test
    fun `GIVEN a priced vault WHEN convert THEN the row shows the title and a balance`() {
        // Act
        val actual = converter.convert(value = onboarded().toAccountStatus())

        // Assert
        val state = actual?.state as TangemAccountRow.State.Content
        assertThat(state.title).isEqualTo(resourceReference(R.string.collectibles_main_title))
        assertThat(state.balance).isNotNull()
        assertThat(state.isBalanceFlickering).isFalse()
    }

    @Test
    fun `GIVEN a converted row WHEN clicked THEN the collectibles account of that wallet is opened`() {
        // Arrange
        val state = converter.convert(value = onboarded().toAccountStatus())?.state as TangemAccountRow.State.Content

        // Act
        state.onClick?.invoke()

        // Assert
        verify(exactly = 1) { onAccountClick(userWalletId) }
    }

    /** The backend lists the account but no vault stands behind it: the wallet is offered onboarding instead */
    @Test
    fun `GIVEN a wallet not onboarded WHEN convert THEN there is no row`() {
        // Act
        val actual = converter.convert(value = CollectiblesAccountStatusValue.NotOnboarded.toAccountStatus())

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a missed derivation WHEN convert THEN the inert row asks to reconnect`() {
        // Act
        val actual = converter.convert(value = CollectiblesAccountStatusValue.MissedDerivation.toAccountStatus())

        // Assert
        val state = actual?.state as TangemAccountRow.State.NotActive
        assertThat(state.description).isEqualTo(resourceReference(R.string.collectibles_account_reconnect_subtitle))
        assertThat(state.onClick).isNull()
    }

    private fun CollectiblesAccountStatusValue.toAccountStatus() = AccountStatus.Collectibles(
        account = Account.Collectibles(userWalletId),
        value = this,
    )

    private fun onboarded(vaultBalance: CollectiblesVaultBalance? = pricedBalance) =
        CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL, vaultBalance = vaultBalance)

    internal data class StateModel(val status: CollectiblesAccountStatusValue, val expected: Class<*>)

    private fun provideTestModels() = listOf(
        StateModel(status = CollectiblesAccountStatusValue.Loading, expected = shimmer),
        StateModel(status = CollectiblesAccountStatusValue.MissedDerivation, expected = notActive),
        StateModel(status = CollectiblesAccountStatusValue.Error.Unavailable, expected = balanceError),
        // the vault exists but has not been read on this device, so there is no amount to show
        StateModel(status = onboarded(vaultBalance = null), expected = balanceError),
        // something is in the vault and nothing can price it, so there is still no amount to show
        StateModel(status = onboarded(vaultBalance = pricedBalance.copy(usdcFiatRate = null)), expected = balanceError),
        StateModel(status = onboarded(), expected = content),
        StateModel(
            status = CollectiblesAccountStatusValue.UpdateRequired(
                source = StatusSource.ACTUAL,
                vaultBalance = pricedBalance,
            ),
            expected = content,
        ),
    )

    private companion object {
        val userWalletId = UserWalletId("011")
        val accountId = Account.Collectibles(userWalletId).accountId.value

        val shimmer = TangemAccountRow.State.Shimmer::class.java
        val notActive = TangemAccountRow.State.NotActive::class.java
        val balanceError = TangemAccountRow.State.BalanceError::class.java
        val content = TangemAccountRow.State.Content::class.java

        val pricedBalance = MockCollectibles.vaultBalance(amount = BigDecimal("2"), fiatRate = BigDecimal("150"))
    }
}