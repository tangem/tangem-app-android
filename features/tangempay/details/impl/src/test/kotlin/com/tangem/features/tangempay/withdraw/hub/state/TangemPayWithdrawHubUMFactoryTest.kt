package com.tangem.features.tangempay.withdraw.hub.state

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.RecentRecipientUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.RecentsUM
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayWithdrawHubUMFactoryTest {

    private val onBackClick: () -> Unit = mockk(relaxed = true)
    private val onWithinPortfolioClick: () -> Unit = mockk(relaxed = true)
    private val onToPersonalAccountClick: () -> Unit = mockk(relaxed = true)
    private val onToBusinessAccountClick: () -> Unit = mockk(relaxed = true)
    private val onRecentRecipientClick: (String) -> Unit = mockk(relaxed = true)
    private val onSeeAllClick: () -> Unit = mockk(relaxed = true)
    private val onRetryRecentsClick: () -> Unit = mockk(relaxed = true)

    private val factory = TangemPayWithdrawHubUMFactory(
        callbacks = TangemPayWithdrawHubUMFactory.Callbacks(
            onBackClick = onBackClick,
            onWithinPortfolioClick = onWithinPortfolioClick,
            onToPersonalAccountClick = onToPersonalAccountClick,
            onToBusinessAccountClick = onToBusinessAccountClick,
            onRecentRecipientClick = onRecentRecipientClick,
            onSeeAllClick = onSeeAllClick,
            onRetryRecentsClick = onRetryRecentsClick,
        ),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            onBackClick,
            onWithinPortfolioClick,
            onToPersonalAccountClick,
            onToBusinessAccountClick,
            onRecentRecipientClick,
            onSeeAllClick,
            onRetryRecentsClick,
        )
    }

    @Test
    fun `GIVEN factory WHEN create THEN within portfolio card has its title`() {
        // Act
        val actual = factory.create()

        // Assert
        assertThat(actual.swapSection.withinPortfolio.title)
            .isEqualTo(resourceReference(R.string.tangempay_withdraw_hub_within_portfolio))
    }

    @Test
    fun `GIVEN factory WHEN create THEN bank cards titled and recents loading`() {
        // Act
        val actual = factory.create()

        // Assert
        assertThat(actual.bankSection.toPersonalAccount.title)
            .isEqualTo(resourceReference(R.string.tangempay_withdraw_hub_to_personal_account))
        assertThat(actual.bankSection.toBusinessAccount.title)
            .isEqualTo(resourceReference(R.string.tangempay_withdraw_hub_to_business_account))
        assertThat(actual.bankSection.recents).isEqualTo(RecentsUM.Loading)
    }

    @Test
    fun `GIVEN created state WHEN within portfolio clicked THEN callback invoked`() {
        // Arrange
        val state = factory.create()

        // Act
        state.swapSection.withinPortfolio.onClick()

        // Assert
        verify(exactly = 1) { onWithinPortfolioClick() }
    }

    @Test
    fun `GIVEN created state WHEN back clicked THEN callback invoked`() {
        // Arrange
        val state = factory.create()

        // Act
        state.onBackClick()

        // Assert
        verify(exactly = 1) { onBackClick() }
    }

    @Test
    fun `GIVEN created state WHEN to personal account clicked THEN callback invoked`() {
        // Arrange
        val state = factory.create()

        // Act
        state.bankSection.toPersonalAccount.onClick()

        // Assert
        verify(exactly = 1) { onToPersonalAccountClick() }
    }

    @Test
    fun `GIVEN created state WHEN to business account clicked THEN callback invoked`() {
        // Arrange
        val state = factory.create()

        // Act
        state.bankSection.toBusinessAccount.onClick()

        // Assert
        verify(exactly = 1) { onToBusinessAccountClick() }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN beneficiary WHEN createRecents THEN short name and initial`(model: NameModel) {
        // Arrange
        val account = bankAccount(id = "acc-1", beneficiary = model.beneficiary, accountNumber = "1234567890")

        // Act
        val actual = factory.createRecents(listOf(account)) as RecentsUM.Content

        // Assert
        val recipient = actual.recipients.single()
        assertThat(recipient).isEqualTo(
            RecentRecipientUM(
                id = "acc-1",
                initial = model.expectedInitial,
                accent = recipient.accent,
                name = model.expectedName,
                accountNumber = "1234567890",
            ),
        )
    }

    @Test
    fun `GIVEN no accounts WHEN createRecents THEN empty`() {
        // Act
        val actual = factory.createRecents(emptyList())

        // Assert
        assertThat(actual).isEqualTo(RecentsUM.Empty)
    }

    @Test
    fun `GIVEN accounts WHEN createRecents THEN order preserved`() {
        // Arrange
        val accounts = listOf(
            bankAccount(id = "acc-2", beneficiary = Beneficiary.Business(name = "Raxanax")),
            bankAccount(id = "acc-1"),
        )

        // Act
        val actual = factory.createRecents(accounts) as RecentsUM.Content

        // Assert
        assertThat(actual.recipients.map { it.id }).containsExactly("acc-2", "acc-1").inOrder()
    }

    @Test
    fun `GIVEN same account id WHEN createRecents twice THEN same avatar accent`() {
        // Arrange
        val account = bankAccount(id = "acc-42")

        // Act
        val first = factory.createRecents(listOf(account)) as RecentsUM.Content
        val second = factory.createRecents(listOf(account)) as RecentsUM.Content

        // Assert
        assertThat(first.recipients.single().accent).isEqualTo(second.recipients.single().accent)
    }

    @Test
    fun `GIVEN content WHEN recipient clicked THEN callback receives account id`() {
        // Arrange
        val content = factory.createRecents(listOf(bankAccount(id = "acc-7"))) as RecentsUM.Content

        // Act
        content.onRecipientClick("acc-7")

        // Assert
        verify(exactly = 1) { onRecentRecipientClick("acc-7") }
    }

    @Test
    fun `GIVEN content WHEN see all clicked THEN callback invoked`() {
        // Arrange
        val content = factory.createRecents(listOf(bankAccount())) as RecentsUM.Content

        // Act
        content.onSeeAllClick()

        // Assert
        verify(exactly = 1) { onSeeAllClick() }
    }

    @Test
    fun `GIVEN error state WHEN retry clicked THEN callback invoked`() {
        // Arrange
        val error = factory.createRecentsError() as RecentsUM.Error

        // Act
        error.onRetryClick()

        // Assert
        verify(exactly = 1) { onRetryRecentsClick() }
    }

    data class NameModel(
        val beneficiary: Beneficiary,
        val expectedName: String,
        val expectedInitial: String,
    )

    private fun provideTestModels() = listOf(
        NameModel(
            beneficiary = Beneficiary.Individual(firstName = "Andrei", lastName = "Melnikov"),
            expectedName = "Andrei M.",
            expectedInitial = "A",
        ),
        NameModel(
            beneficiary = Beneficiary.Individual(firstName = "Johnny", lastName = ""),
            expectedName = "Johnny",
            expectedInitial = "J",
        ),
        NameModel(
            beneficiary = Beneficiary.Individual(firstName = "Pablo", lastName = " escobar"),
            expectedName = "Pablo E.",
            expectedInitial = "P",
        ),
        NameModel(
            beneficiary = Beneficiary.Business(name = "raxanax LLC"),
            expectedName = "raxanax LLC",
            expectedInitial = "R",
        ),
    )
}