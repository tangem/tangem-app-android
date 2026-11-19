package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.tangempay.TangemPayCard
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.child.wallet.model.intents.WalletContentClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayGroupUM
import com.tangem.features.moneyaccount.main.entity.MoneyAccountMainUM
import com.tangem.features.tangempay.entity.TangemPayMainUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.StringsSigns.DASH_SIGN
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayGroupConverterTest {

    private val clickIntents: WalletContentClickIntents = mockk(relaxed = true)
    private val paymentAccount = Account.Payment(UserWalletId("0011"))
    private val paymentAccountId: AccountId = paymentAccount.accountId

    @BeforeEach
    fun resetMocks() {
        clearMocks(clickIntents)
    }

    @Test
    fun `GIVEN no money account WHEN convert THEN hidden`() {
        // Act
        val result = converter(expandedAccounts = emptySet()).convert(
            paymentStatus = paymentStatus(loadedValue(balance = BigDecimal.ONE)),
            moneyStatus = null,
            tangemPayMainUM = TangemPayMainUM.Loading,
            moneyAccountMainUM = MoneyAccountMainUM.Unavailable,
        )

        // Assert
        assertThat(result).isEqualTo(TangemPayGroupUM.Hidden)
    }

    @Test
    fun `GIVEN tangem pay block is empty WHEN convert THEN hidden`() {
        // Act
        val result = converter(expandedAccounts = emptySet()).convert(
            paymentStatus = paymentStatus(loadedValue(balance = BigDecimal.ONE)),
            moneyStatus = moneyStatus(activeValue(balance = BigDecimal.ONE)),
            tangemPayMainUM = TangemPayMainUM.Empty,
            moneyAccountMainUM = MoneyAccountMainUM.Unavailable,
        )

        // Assert
        assertThat(result).isEqualTo(TangemPayGroupUM.Hidden)
    }

    @Test
    fun `GIVEN both balances and collapsed group WHEN convert and click THEN sum shown and collapsed state passed`() {
        // Arrange
        val converter = converter(expandedAccounts = emptySet())

        // Act
        val result = converter.convert(
            paymentStatus = paymentStatus(loadedValue(balance = BigDecimal("1545.35"))),
            moneyStatus = moneyStatus(activeValue(balance = BigDecimal("340.00"))),
            tangemPayMainUM = TangemPayMainUM.Loading,
            moneyAccountMainUM = MoneyAccountMainUM.Unavailable,
        ) as TangemPayGroupUM.Content
        result.onClick()

        // Assert
        assertThat(result).isEqualTo(
            expectedContent(
                balance = usd(BigDecimal("1885.35")),
                isBalanceFlickering = false,
                isExpanded = false,
                onClick = result.onClick,
            ),
        )
        verify(exactly = 1) { clickIntents.onTangemPayGroupClick(accountId = paymentAccountId, isExpanded = false) }
    }

    @Test
    fun `GIVEN expanded group WHEN click THEN expanded state passed`() {
        // Arrange
        val converter = converter(expandedAccounts = setOf(paymentAccountId))

        // Act
        val result = converter.convert(
            paymentStatus = paymentStatus(loadedValue(balance = BigDecimal.ONE)),
            moneyStatus = moneyStatus(activeValue(balance = BigDecimal.ONE)),
            tangemPayMainUM = TangemPayMainUM.Loading,
            moneyAccountMainUM = MoneyAccountMainUM.Unavailable,
        ) as TangemPayGroupUM.Content
        result.onClick()

        // Assert
        assertThat(result.isExpanded).isTrue()
        verify(exactly = 1) { clickIntents.onTangemPayGroupClick(accountId = paymentAccountId, isExpanded = true) }
    }

    @Test
    fun `GIVEN tangem pay loading WHEN convert THEN only money balance shown flickering`() {
        // Act
        val result = converter(expandedAccounts = emptySet()).convert(
            paymentStatus = paymentStatus(PaymentAccountStatusValue.Loading),
            moneyStatus = moneyStatus(activeValue(balance = BigDecimal("340.00"))),
            tangemPayMainUM = TangemPayMainUM.Loading,
            moneyAccountMainUM = MoneyAccountMainUM.Unavailable,
        ) as TangemPayGroupUM.Content

        // Assert
        assertThat(result).isEqualTo(
            expectedContent(
                balance = usd(BigDecimal("340.00")),
                isBalanceFlickering = true,
                isExpanded = false,
                onClick = result.onClick,
            ),
        )
    }

    @Test
    fun `GIVEN no balances available WHEN convert THEN dash shown`() {
        // Act
        val result = converter(expandedAccounts = emptySet()).convert(
            paymentStatus = paymentStatus(loadedValue(balance = null)),
            moneyStatus = moneyStatus(MoneyAccountStatusValue.Error.Unavailable),
            tangemPayMainUM = TangemPayMainUM.Loading,
            moneyAccountMainUM = MoneyAccountMainUM.Unavailable,
        ) as TangemPayGroupUM.Content

        // Assert
        assertThat(result.balance).isEqualTo(stringReference(DASH_SIGN))
    }

    @Test
    fun `GIVEN money not started and pay loaded WHEN convert THEN group with pay balance shown`() {
        // Act
        val result = converter(expandedAccounts = emptySet()).convert(
            paymentStatus = paymentStatus(loadedValue(balance = BigDecimal("1545.35"))),
            moneyStatus = moneyStatus(
                MoneyAccountStatusValue.Onboarding.NotStarted(source = StatusSource.ACTUAL, customerId = "customer"),
            ),
            tangemPayMainUM = TangemPayMainUM.Loading,
            moneyAccountMainUM = MoneyAccountMainUM.NotStarted(onDetailsClick = {}),
        ) as TangemPayGroupUM.Content

        // Assert
        assertThat(result).isEqualTo(
            expectedContent(
                balance = usd(BigDecimal("1545.35")),
                isBalanceFlickering = false,
                isExpanded = false,
                onClick = result.onClick,
            ),
        )
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN accounts action required state WHEN convert THEN group action required state matches`(
        model: ActionRequiredModel,
    ) {
        // Act
        val result = converter(expandedAccounts = emptySet()).convert(
            paymentStatus = paymentStatus(loadedValue(balance = BigDecimal("1545.35"))),
            moneyStatus = moneyStatus(activeValue(balance = BigDecimal("340.00"))),
            tangemPayMainUM = model.tangemPayMainUM,
            moneyAccountMainUM = model.moneyAccountMainUM,
        ) as TangemPayGroupUM.Content

        // Assert
        assertThat(result).isEqualTo(
            expectedContent(
                balance = usd(BigDecimal("1885.35")),
                isBalanceFlickering = false,
                isExpanded = false,
                onClick = result.onClick,
                isActionRequired = model.expectedIsActionRequired,
            ),
        )
    }

    private fun provideTestModels() = listOf(
        ActionRequiredModel(
            tangemPayMainUM = tangemPayContent(isActionRequired = true),
            moneyAccountMainUM = moneyAccountContent(isActionRequired = false),
            expectedIsActionRequired = true,
        ),
        ActionRequiredModel(
            tangemPayMainUM = tangemPayContent(isActionRequired = false),
            moneyAccountMainUM = moneyAccountContent(isActionRequired = true),
            expectedIsActionRequired = true,
        ),
        ActionRequiredModel(
            tangemPayMainUM = tangemPayContent(isActionRequired = true),
            moneyAccountMainUM = moneyAccountContent(isActionRequired = true),
            expectedIsActionRequired = true,
        ),
        ActionRequiredModel(
            tangemPayMainUM = tangemPayContent(isActionRequired = false),
            moneyAccountMainUM = moneyAccountContent(isActionRequired = false),
            expectedIsActionRequired = false,
        ),
        ActionRequiredModel(
            tangemPayMainUM = TangemPayMainUM.ActionRequired(onClick = {}),
            moneyAccountMainUM = MoneyAccountMainUM.Unavailable,
            expectedIsActionRequired = false,
        ),
    )

    data class ActionRequiredModel(
        val tangemPayMainUM: TangemPayMainUM,
        val moneyAccountMainUM: MoneyAccountMainUM,
        val expectedIsActionRequired: Boolean,
    )

    private fun tangemPayContent(isActionRequired: Boolean) = TangemPayMainUM.Content(
        subtitle = stringReference("*1234"),
        isBalanceFlickering = false,
        balance = stringReference("$1,545.35"),
        balanceSubtitle = stringReference("USDC"),
        onClick = {},
        shouldShowOnlyCacheWarning = false,
        isActionRequired = isActionRequired,
    )

    private fun moneyAccountContent(isActionRequired: Boolean) = MoneyAccountMainUM.Content(
        subtitle = stringReference("USD account"),
        balance = null,
        isActionRequired = isActionRequired,
        onClick = {},
    )

    private fun converter(expandedAccounts: Set<AccountId>) = TangemPayGroupConverter(
        expandedAccounts = expandedAccounts,
        clickIntents = clickIntents,
    )

    private fun expectedContent(
        balance: TextReference,
        isBalanceFlickering: Boolean,
        isExpanded: Boolean,
        onClick: () -> Unit,
        isActionRequired: Boolean = false,
    ) = TangemPayGroupUM.Content(
        id = "TangemPayGroup",
        title = resourceReference(R.string.tangempay_main_group_title),
        label = if (isActionRequired) {
            resourceReference(R.string.tangempay_action_required_title)
        } else {
            pluralReference(
                id = R.plurals.tangempay_main_group_subtitle,
                count = 2,
                formatArgs = wrappedList(2),
            )
        },
        balance = balance,
        balanceCaption = stringReference("USD"),
        isBalanceFlickering = isBalanceFlickering,
        isExpanded = isExpanded,
        isActionRequired = isActionRequired,
        onClick = onClick,
    )

    private fun usd(amount: BigDecimal): TextReference =
        stringReference(amount.format { fiat(fiatCurrencyCode = "USD", fiatCurrencySymbol = "$") })

    private fun paymentStatus(value: PaymentAccountStatusValue) =
        AccountStatus.Payment(account = paymentAccount, value = value)

    private fun moneyStatus(value: MoneyAccountStatusValue) =
        AccountStatus.Money(account = Account.Money(UserWalletId("0011")), value = value)

    private fun loadedValue(balance: BigDecimal?) = PaymentAccountStatusValue.Loaded(
        source = StatusSource.ACTUAL,
        customerId = "customer",
        paymentAccountAddress = "0xdeposit",
        balance = balance?.let {
            PaymentAccountStatusValue.Balance(
                fiatBalance = PaymentAccountStatusValue.FiatBalance(availableBalance = it, currency = "USD"),
            )
        },
        cryptoCurrency = mockk(relaxed = true),
        networks = emptyList(),
        cards = listOf(mockk<TangemPayCard>()),
        fiatRate = null,
        error = null,
        virtualAccount = null,
        isBankOfframpAvailable = null,
        tariffPlan = null,
        isPaymentAccountIssuing = false,
        isKycAdditionalInfoRequired = false,
    )

    private fun activeValue(balance: BigDecimal) = MoneyAccountStatusValue.Account.Active(
        source = StatusSource.ACTUAL,
        cryptoCurrency = mockk(relaxed = true),
        balance = MoneyAccountStatusValue.Balance(
            cryptoAmount = balance,
            depositAddress = "0xdeposit",
            currencyCode = "USD",
            fiatRate = BigDecimal.ONE,
        ),
        customerId = "customer",
        productInstanceId = "instance",
        error = null,
        isBankOfframpAvailable = null,
        isKycAdditionalInfoRequired = false,
    )
}