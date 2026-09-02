package com.tangem.features.tangempay.account

import com.tangem.domain.models.pay.TangemPayImage
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanState
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.features.tangempay.addFundsButton
import com.tangem.features.tangempay.awaitingDepositOrder
import com.tangem.features.tangempay.customerTariffPlan
import com.tangem.features.tangempay.recurringFee
import com.tangem.features.tangempay.tariffPlan
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.tangemPayCard
import com.tangem.features.tangempay.tariffPlanState
import com.tangem.features.tangempay.withdrawButton
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import kotlin.reflect.KClass

internal class TangemPayDetailsStateFactoryTest {

    private val intents: TangemPayDetailIntents = mockk(relaxed = true)

    private val activeUnfrozenCard = tangemPayCard()

    private val availableNetwork: PaymentNetworkStatus.Available = mockk(relaxed = true)

    private val notIssuedNetwork: PaymentNetworkStatus.NotIssued = mockk(relaxed = true)

    private val factory = TangemPayDetailsStateFactory(
        onBack = {},
        onOpenMenu = {},
        intents = intents,
        isTiersPlusPlanEnabled = true,
        isMultichainEnabled = true,
    )

    private val singleNetworkFactory = TangemPayDetailsStateFactory(
        onBack = {},
        onOpenMenu = {},
        intents = intents,
        isTiersPlusPlanEnabled = true,
        isMultichainEnabled = false,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(intents)
    }

    @Test
    fun `GIVEN multichain enabled WHEN building any state THEN top bar subtitle is Multinetwork`() {
        // Arrange
        val status = loadedStatus(statusSource = StatusSource.ACTUAL, statusError = null)
        val expected = resourceReference(R.string.tangempay_multinetwork)

        // Act & Assert
        assertThat(factory.getLoadingState().topBarConfig.subtitle).isEqualTo(expected)
        assertThat(factory.getLoadedState(status).topBarConfig.subtitle).isEqualTo(expected)
    }

    @Test
    fun `GIVEN multichain disabled WHEN building any state THEN top bar subtitle names the single network`() {
        // Arrange
        val expected = resourceReference(R.string.tangempay_usdc_on_polygon_network)

        // Act
        val subtitle = singleNetworkFactory.getLoadingState().topBarConfig.subtitle

        // Assert
        assertThat(subtitle).isEqualTo(expected)
    }

    @ParameterizedTest
    @MethodSource("provideButtonStateCases")
    fun `GIVEN status source WHEN getLoadedState THEN action buttons gated by freshness but card tile only by error`(
        case: ButtonStateCase,
    ) {
        // Arrange
        val status = loadedStatus(statusSource = case.source, statusError = case.error)

        // Act
        val state = factory.getLoadedState(status)

        // Assert
        val actionButtonsEnabled = state.balanceBlockState.actionButtons.map { it.config.isEnabled }
        assertThat(actionButtonsEnabled).containsExactly(case.expectedEnabled, case.expectedEnabled)
        assertThat(state.balanceBlockState.cardsBlockState?.isAddCardEnabled).isEqualTo(case.expectedEnabled)
        // The card tile is intentionally NOT source-gated: it stays clickable on stale data as long as
        // there is no error, so the user can still view the card details offline (which self-gate actions).
        assertThat(state.balanceBlockState.cardsBlockState?.cards?.single()?.isEnabled)
            .isEqualTo(case.expectedCardEnabled)
    }

    @Test
    fun `GIVEN actual status with only frozen card WHEN getLoadedState THEN action buttons disabled`() {
        // Arrange
        val frozenCard = activeUnfrozenCard.copy(frozenState = TangemPayCardFrozenState.Frozen)
        val status = loadedStatus(
            statusSource = StatusSource.ACTUAL,
            statusError = null,
            statusCards = listOf(frozenCard),
        )

        // Act
        val state = factory.getLoadedState(status)

        // Assert
        assertThat(state.balanceBlockState.actionButtons.map { it.config.isEnabled }).containsExactly(false, false)
        assertThat(state.balanceBlockState.cardsBlockState?.isAddCardEnabled).isTrue()
    }

    @Test
    fun `GIVEN actual status with issuing card WHEN getLoadedState THEN add card disabled`() {
        // Arrange
        val issuingCard = activeUnfrozenCard.copy(state = TangemPayCardState.Issuing)
        val status = loadedStatus(
            statusSource = StatusSource.ACTUAL,
            statusError = null,
            statusCards = listOf(activeUnfrozenCard, issuingCard),
        )

        // Act
        val state = factory.getLoadedState(status)

        // Assert
        assertThat(state.balanceBlockState.cardsBlockState?.isAddCardEnabled).isFalse()
    }

    @ParameterizedTest
    @MethodSource("provideBalanceCases")
    fun `GIVEN fresh status WHEN getLoadedState THEN withdraw gated by balance but add funds enabled`(
        case: BalanceCase,
    ) {
        // Arrange
        val status = loadedStatus(availableForWithdrawal = case.availableForWithdrawal)

        // Act
        val state = factory.getLoadedState(status)

        // Assert
        assertThat(state.addFundsButton.isEnabled).isTrue()
        assertThat(state.withdrawButton.isEnabled).isEqualTo(case.expectedWithdrawEnabled)
    }

    @ParameterizedTest
    @MethodSource("provideAddFundsCases")
    fun `GIVEN deposit destination WHEN getLoadedState THEN add funds gated by it`(case: AddFundsCase) {
        // Arrange
        val status = loadedStatus(
            statusDepositAddress = case.depositAddress,
            statusNetworks = case.networks.map { isAvailable ->
                if (isAvailable) availableNetwork else notIssuedNetwork
            },
        )
        val stateFactory = if (case.isMultichainEnabled) factory else singleNetworkFactory

        // Act
        val state = stateFactory.getLoadedState(status)

        // Assert
        assertThat(state.addFundsButton.isEnabled).isEqualTo(case.expectedAddFundsEnabled)
        assertThat(state.withdrawButton.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN deactivated with positive balance WHEN getDeactivatedState THEN withdraw enabled`() {
        // Act
        val state = factory.getDeactivatedState(deactivatedStatus(availableForWithdrawal = BigDecimal.TEN))

        // Assert
        assertThat(state.addFundsButton.isEnabled).isTrue()
        assertThat(state.withdrawButton.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN deactivated with zero balance WHEN getDeactivatedState THEN withdraw disabled`() {
        // Act
        val state = factory.getDeactivatedState(deactivatedStatus(availableForWithdrawal = BigDecimal.ZERO))

        // Assert
        assertThat(state.addFundsButton.isEnabled).isTrue()
        assertThat(state.withdrawButton.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN deactivated without issued networks WHEN getDeactivatedState THEN add funds disabled`() {
        // Arrange
        val status = deactivatedStatus(
            availableForWithdrawal = BigDecimal.ZERO,
            statusNetworks = listOf(notIssuedNetwork),
        )

        // Act
        val state = factory.getDeactivatedState(status)

        // Assert
        assertThat(state.addFundsButton.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN deactivated with deposit address WHEN getDeactivatedState THEN add funds enabled`() {
        // Arrange
        val status = deactivatedStatus(availableForWithdrawal = BigDecimal.ZERO, statusNetworks = emptyList())

        // Act
        val state = singleNetworkFactory.getDeactivatedState(status)

        // Assert
        assertThat(state.addFundsButton.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN plan transition in progress WHEN getLoadedState THEN current plan menu item stays clickable`() {
        // Arrange
        val planState = tariffPlanState(order = awaitingDepositOrder())
        val status = loadedStatus(statusTariffPlan = planState)

        // Act
        val state = factory.getLoadedState(status)

        // Assert
        val currentPlanItem = state.topBarConfig.items.first()
        assertThat(currentPlanItem.title).isEqualTo(resourceReference(R.string.tangempay_current_plan_title))
        assertThat(currentPlanItem.isEnabled).isTrue()
        assertThat(currentPlanItem.subtitle).isEqualTo(resourceReference(R.string.tangempay_changing_plan))
        currentPlanItem.onClick()
        verify(exactly = 1) { intents.onClickCurrentPlan(planState) }
    }

    @Test
    fun `GIVEN no plan transition WHEN getLoadedState THEN current plan menu item shows plan name`() {
        // Arrange
        val planState = tariffPlanState()
        val status = loadedStatus(statusTariffPlan = planState)

        // Act
        val state = factory.getLoadedState(status)

        // Assert
        val currentPlanItem = state.topBarConfig.items.first()
        assertThat(currentPlanItem.isEnabled).isTrue()
        assertThat(currentPlanItem.subtitle).isEqualTo(stringReference(planState.tariff.plan.name))
    }

    @Test
    fun `GIVEN card issue failed WHEN getCardIssueFailedState THEN error banner and add card element are shown`() {
        // Arrange
        val status = cardIssueFailedStatus(planState = tariffPlanState())

        // Act
        val state = factory.getCardIssueFailedState(status, isBannerDismissed = false)

        // Assert
        val banner = state.statusBannerState
        assertThat(banner?.state?.title).isEqualTo(resourceReference(R.string.tangempay_failed_to_issue_card))
        assertThat(banner?.state?.variant).isEqualTo(TangemMessageBanner.Variant.Error)
        assertThat(banner?.state?.secondaryButton?.text)
            .isEqualTo(resourceReference(R.string.common_contact_support))
        banner?.onClose?.invoke()
        banner?.state?.secondaryButton?.onClick?.invoke()
        verify(exactly = 1) { intents.onCardIssueFailedBannerDismissed() }
        verify(exactly = 1) { intents.onCardIssueFailedSupportClick() }
        assertThat(state.errorNotificationConfig).isNull()
        assertThat(state.balanceBlockState.cardsBlockState?.cards).isEmpty()
        assertThat(state.balanceBlockState.cardsBlockState?.isAddCardEnabled).isTrue()
        assertThat(state.addFundsButton.isEnabled).isFalse()
        assertThat(state.withdrawButton.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN card issue failed WHEN getCardIssueFailedState THEN zero balance in the plan currency is shown`() {
        // Arrange
        val eurPlan = tariffPlanState(
            tariff = customerTariffPlan(plan = tariffPlan(fees = listOf(recurringFee(currency = "EUR")))),
        )
        val status = cardIssueFailedStatus(planState = eurPlan)

        // Act
        val balanceBlock = factory.getCardIssueFailedState(status, isBannerDismissed = false).balanceBlockState

        // Assert
        assertThat(balanceBlock).isInstanceOf(TangemPayDetailsBalanceBlockState.Content::class.java)
        val content = balanceBlock as TangemPayDetailsBalanceBlockState.Content
        assertThat(content.isInactive).isFalse()
        assertThat(content.fiatBalance).isEqualTo(zeroBalanceText(currency = "EUR"))
    }

    @Test
    fun `GIVEN card issue failed without a plan WHEN getCardIssueFailedState THEN zero balance falls back to USD`() {
        // Arrange
        val status = cardIssueFailedStatus(planState = null)

        // Act
        val balanceBlock = factory.getCardIssueFailedState(status, isBannerDismissed = false).balanceBlockState

        // Assert
        assertThat((balanceBlock as TangemPayDetailsBalanceBlockState.Content).fiatBalance)
            .isEqualTo(zeroBalanceText(currency = "USD"))
    }

    private fun zeroBalanceText(currency: String) = DetailsBalanceTransformer.getFiatBalanceText(
        PaymentAccountStatusValue.FiatBalance(availableBalance = BigDecimal.ZERO, currency = currency),
    )

    @Test
    fun `GIVEN card issue failed with a deposit address WHEN getCardIssueFailedState THEN add funds is enabled`() {
        // Arrange
        val status = cardIssueFailedStatus(
            planState = tariffPlanState(),
            statusBalance = balance(availableForWithdrawal = BigDecimal.TEN),
        )

        // Act
        val state = singleNetworkFactory.getCardIssueFailedState(status, isBannerDismissed = false)

        // Assert
        assertThat(state.addFundsButton.isEnabled).isTrue()
        assertThat(state.withdrawButton.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN dismissed banner WHEN getCardIssueFailedState THEN the banner is hidden`() {
        // Arrange
        val status = cardIssueFailedStatus(planState = tariffPlanState())

        // Act
        val state = factory.getCardIssueFailedState(status, isBannerDismissed = true)

        // Assert
        assertThat(state.statusBannerState).isNull()
    }

    @Test
    fun `GIVEN card issue failed with a plan WHEN getCardIssueFailedState THEN current plan menu item is present`() {
        // Arrange
        val planState = tariffPlanState()
        val status = cardIssueFailedStatus(planState = planState)

        // Act
        val state = factory.getCardIssueFailedState(status, isBannerDismissed = false)

        // Assert
        val currentPlanItem = state.topBarConfig.items.first()
        assertThat(currentPlanItem.title).isEqualTo(resourceReference(R.string.tangempay_current_plan_title))
        currentPlanItem.onClick()
        verify(exactly = 1) { intents.onClickCurrentPlan(planState) }
    }

    @Test
    fun `GIVEN card issue failed WHEN add card clicked THEN the standard add card flow is requested`() {
        // Arrange
        val planState = tariffPlanState()
        val state = factory.getCardIssueFailedState(
            status = cardIssueFailedStatus(planState = planState),
            isBannerDismissed = false,
        )

        // Act
        state.balanceBlockState.cardsBlockState?.onAddCardClick?.invoke()

        // Assert
        verify(exactly = 1) { intents.onAddCardClick(planState) }
    }

    @Test
    fun `GIVEN card issue failed without a plan WHEN getCardIssueFailedState THEN current plan item is absent`() {
        // Arrange
        val status = cardIssueFailedStatus(planState = null)

        // Act
        val state = factory.getCardIssueFailedState(status, isBannerDismissed = false)

        // Assert
        assertThat(state.topBarConfig.items.map { it.title })
            .doesNotContain(resourceReference(R.string.tangempay_current_plan_title))
    }

    @Test
    fun `GIVEN withdraw disabled WHEN getActionButtonsConfig THEN withdraw disabled and add funds enabled`() {
        // Act
        val buttons = factory.getActionButtonsConfig(isAddFundsEnabled = true, isWithdrawEnabled = false)

        // Assert
        assertThat(buttons.addFundsButton.isEnabled).isTrue()
        assertThat(buttons.withdrawButton.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN both enabled WHEN getActionButtonsConfig THEN both buttons enabled`() {
        // Act
        val buttons = factory.getActionButtonsConfig(isAddFundsEnabled = true, isWithdrawEnabled = true)

        // Assert
        assertThat(buttons.addFundsButton.isEnabled).isTrue()
        assertThat(buttons.withdrawButton.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN loaded status without balance WHEN getLoadedState THEN balance block is Error with cards kept`() {
        // Arrange
        val status = loadedStatus(statusBalance = null)

        // Act
        val state = factory.getLoadedState(status)

        // Assert
        assertThat(state.balanceBlockState).isInstanceOf(TangemPayDetailsBalanceBlockState.Error::class.java)
        assertThat(state.balanceBlockState.actionButtons.map { it.config.isEnabled }).containsExactly(false, false)
        assertThat(state.balanceBlockState.cardsBlockState?.cards).hasSize(1)
    }

    @Test
    fun `GIVEN deactivated status without balance WHEN getDeactivatedState THEN balance block is Error`() {
        // Arrange
        val status = deactivatedStatus(availableForWithdrawal = BigDecimal.ZERO, statusBalance = null)

        // Act
        val state = factory.getDeactivatedState(status)

        // Assert
        assertThat(state.balanceBlockState).isInstanceOf(TangemPayDetailsBalanceBlockState.Error::class.java)
        assertThat(state.balanceBlockState.actionButtons.map { it.config.isEnabled }).containsExactly(false, false)
    }

    @ParameterizedTest
    @MethodSource("provideProgressBannerCases")
    fun `GIVEN card states WHEN getLoadedState THEN progress banner resolved`(case: ProgressBannerCase) {
        // Arrange
        val status = loadedStatus(statusCards = case.cards)

        // Act
        val state = factory.getLoadedState(status)

        // Assert
        val banner = state.balanceBlockState.cardsBlockState?.progressBanner
        if (case.expectedBanner == null) {
            assertThat(banner).isNull()
        } else {
            assertThat(banner).isInstanceOf(case.expectedBanner.java)
        }
    }

    @Test
    fun `GIVEN exactly one delivering card WHEN banner activate clicked THEN opens activation for that card`() {
        // Arrange
        val status = loadedStatus(
            statusCards = listOf(
                activeUnfrozenCard,
                tangemPayCard(id = "plastic", state = TangemPayCardState.Delivering),
            ),
        )

        // Act
        val banner = factory.getLoadedState(status).balanceBlockState.cardsBlockState?.progressBanner
        (banner as CardsProgressBannerUM.Delivering).onActivateClick!!.invoke()

        // Assert
        verify(exactly = 1) { intents.onActivateCardClick("plastic") }
        verify(exactly = 0) { intents.onCardClick(any()) }
    }

    @Test
    fun `GIVEN a single delivering placeholder WHEN state built THEN the banner has no activation action`() {
        // Arrange
        val status = loadedStatus(
            statusCards = listOf(
                activeUnfrozenCard,
                tangemPayCard(id = "order_1", state = TangemPayCardState.Delivering, isPlaceholder = true),
            ),
        )

        // Act
        val banner = factory.getLoadedState(status).balanceBlockState.cardsBlockState?.progressBanner

        // Assert
        assertThat((banner as CardsProgressBannerUM.Delivering).onActivateClick).isNull()
    }

    @ParameterizedTest
    @MethodSource("provideCardTileCases")
    fun `GIVEN card state WHEN getLoadedState THEN tile hides card identity until activation`(case: CardTileCase) {
        // Arrange
        val status = loadedStatus(statusCards = listOf(case.card))

        // Act
        val tile = factory.getLoadedState(status).balanceBlockState.cardsBlockState?.cards?.single()

        // Assert
        assertThat(tile?.lastDigits).isEqualTo(case.expectedLastDigits)
        assertThat(tile?.imageUrl).isEqualTo(case.expectedImageUrl)
        assertThat(tile?.isFrozen).isEqualTo(case.expectedFrozen)
        assertThat(tile?.state).isEqualTo(case.expectedUiState)
    }

    internal data class ProgressBannerCase(
        val name: String,
        val cards: List<TangemPayCard>,
        val expectedBanner: KClass<out CardsProgressBannerUM>?,
    ) {
        override fun toString(): String = name
    }

    internal data class CardTileCase(
        val name: String,
        val card: TangemPayCard,
        val expectedLastDigits: String,
        val expectedImageUrl: String?,
        val expectedFrozen: Boolean,
        val expectedUiState: TangemPayCardUiState,
    ) {
        override fun toString(): String = name
    }

    private fun loadedStatus(
        statusSource: StatusSource = StatusSource.ACTUAL,
        statusError: PaymentAccountStatusValue.Error? = null,
        statusCards: List<TangemPayCard> = listOf(activeUnfrozenCard),
        availableForWithdrawal: BigDecimal = BigDecimal.TEN,
        statusTariffPlan: TangemPayTariffPlanState? = null,
        statusBalance: PaymentAccountStatusValue.Balance? = balance(availableForWithdrawal),
        statusDepositAddress: String? = "address",
        statusNetworks: List<PaymentNetworkStatus> = listOf(availableNetwork),
    ): PaymentAccountStatusValue.Loaded = mockk(relaxed = true) {
        every { source } returns statusSource
        every { error } returns statusError
        every { cards } returns statusCards
        every { balance } returns statusBalance
        every { tariffPlan } returns statusTariffPlan
        every { depositAddress } returns statusDepositAddress
        every { networks } returns statusNetworks
    }

    private fun deactivatedStatus(
        availableForWithdrawal: BigDecimal,
        statusBalance: PaymentAccountStatusValue.Balance? = balance(availableForWithdrawal),
        statusNetworks: List<PaymentNetworkStatus> = listOf(availableNetwork),
    ): PaymentAccountStatusValue.Deactivated =
        mockk(relaxed = true) {
            every { source } returns StatusSource.ACTUAL
            every { balance } returns statusBalance
            every { networks } returns statusNetworks
        }

    private fun cardIssueFailedStatus(
        planState: TangemPayTariffPlanState?,
        statusBalance: PaymentAccountStatusValue.Balance? = null,
    ): PaymentAccountStatusValue.Error.CardIssueFailed = PaymentAccountStatusValue.Error.CardIssueFailed(
        customerId = "cust_1",
        tariffPlan = planState,
        balance = statusBalance,
    )

    private fun balance(availableForWithdrawal: BigDecimal) = PaymentAccountStatusValue.Balance(
        fiatBalance = PaymentAccountStatusValue.FiatBalance(
            availableBalance = BigDecimal.ZERO,
            currency = "USD",
        ),
        cryptoBalance = PaymentAccountStatusValue.CryptoBalance(
            id = "id",
            chainId = 1L,
            depositAddress = "address",
            tokenContractAddress = "contract",
            balance = BigDecimal.ZERO,
        ),
        availableForWithdrawal = availableForWithdrawal,
    )

    internal data class ButtonStateCase(
        val source: StatusSource,
        val error: PaymentAccountStatusValue.Error?,
        val expectedEnabled: Boolean,
        val expectedCardEnabled: Boolean,
    )

    internal data class BalanceCase(
        val availableForWithdrawal: BigDecimal,
        val expectedWithdrawEnabled: Boolean,
    )

    internal data class AddFundsCase(
        val name: String,
        val isMultichainEnabled: Boolean,
        val depositAddress: String?,
        val networks: List<Boolean>,
        val expectedAddFundsEnabled: Boolean,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        @JvmStatic
        fun provideAddFundsCases() = listOf(
            AddFundsCase(
                name = "multichain with an issued network -> enabled",
                isMultichainEnabled = true,
                depositAddress = "address",
                networks = listOf(false, true),
                expectedAddFundsEnabled = true,
            ),
            AddFundsCase(
                name = "multichain without an issued network -> disabled",
                isMultichainEnabled = true,
                depositAddress = "address",
                networks = listOf(false),
                expectedAddFundsEnabled = false,
            ),
            AddFundsCase(
                name = "multichain without networks -> disabled",
                isMultichainEnabled = true,
                depositAddress = "address",
                networks = emptyList(),
                expectedAddFundsEnabled = false,
            ),
            AddFundsCase(
                name = "multichain with an issued network and no legacy address -> enabled",
                isMultichainEnabled = true,
                depositAddress = null,
                networks = listOf(true),
                expectedAddFundsEnabled = true,
            ),
            AddFundsCase(
                name = "single network with a deposit address -> enabled",
                isMultichainEnabled = false,
                depositAddress = "address",
                networks = emptyList(),
                expectedAddFundsEnabled = true,
            ),
            AddFundsCase(
                name = "single network without a deposit address -> disabled",
                isMultichainEnabled = false,
                depositAddress = null,
                networks = listOf(true),
                expectedAddFundsEnabled = false,
            ),
            AddFundsCase(
                name = "single network with a blank deposit address -> disabled",
                isMultichainEnabled = false,
                depositAddress = "",
                networks = listOf(true),
                expectedAddFundsEnabled = false,
            ),
        )

        @JvmStatic
        fun provideBalanceCases() = listOf(
            BalanceCase(availableForWithdrawal = BigDecimal.ZERO, expectedWithdrawEnabled = false),
            BalanceCase(availableForWithdrawal = BigDecimal.TEN, expectedWithdrawEnabled = true),
            BalanceCase(availableForWithdrawal = BigDecimal("-1"), expectedWithdrawEnabled = false),
        )

        @JvmStatic
        fun provideButtonStateCases() = listOf(
            // Fresh data from the network -> actions allowed, card tile clickable.
            ButtonStateCase(
                source = StatusSource.ACTUAL,
                error = null,
                expectedEnabled = true,
                expectedCardEnabled = true,
            ),
            // Cache restored from disk before a refresh confirms it -> actions blocked, card tile clickable.
            ButtonStateCase(
                source = StatusSource.CACHE,
                error = null,
                expectedEnabled = false,
                expectedCardEnabled = true,
            ),
            // Internet unavailable, only cache left ([REDACTED_TASK_KEY] case 1) -> actions blocked, card tile clickable.
            ButtonStateCase(
                source = StatusSource.ONLY_CACHE,
                error = null,
                expectedEnabled = false,
                expectedCardEnabled = true,
            ),
            // Expired refresh token, only cache left ([REDACTED_TASK_KEY] case 2) -> everything blocked by the error.
            ButtonStateCase(
                source = StatusSource.ONLY_CACHE,
                error = PaymentAccountStatusValue.Error.NotSynced,
                expectedEnabled = false,
                expectedCardEnabled = false,
            ),
            // Transient error overlaid on actual data -> everything blocked by the error.
            ButtonStateCase(
                source = StatusSource.ACTUAL,
                error = PaymentAccountStatusValue.Error.Unavailable,
                expectedEnabled = false,
                expectedCardEnabled = false,
            ),
        )

        @JvmStatic
        fun provideProgressBannerCases() = listOf(
            ProgressBannerCase(
                name = "no delivering card -> no banner",
                cards = listOf(tangemPayCard()),
                expectedBanner = null,
            ),
            ProgressBannerCase(
                name = "exactly one delivering card -> delivering banner",
                cards = listOf(tangemPayCard(), deliveringCard(id = "plastic")),
                expectedBanner = CardsProgressBannerUM.Delivering::class,
            ),
            ProgressBannerCase(
                name = "only a delivering card -> delivering banner",
                cards = listOf(deliveringCard(id = "plastic")),
                expectedBanner = CardsProgressBannerUM.Delivering::class,
            ),
            ProgressBannerCase(
                name = "two delivering cards -> multiple delivering banner",
                cards = listOf(deliveringCard(id = "plastic_1"), deliveringCard(id = "plastic_2")),
                expectedBanner = CardsProgressBannerUM.DeliveringMultiple::class,
            ),
            ProgressBannerCase(
                name = "reissuing card wins over delivering",
                cards = listOf(
                    tangemPayCard(id = "virtual", state = TangemPayCardState.Reissuing),
                    deliveringCard(id = "plastic"),
                ),
                expectedBanner = CardsProgressBannerUM.Reissuing::class,
            ),
            ProgressBannerCase(
                name = "issuing card wins over delivering",
                cards = listOf(
                    tangemPayCard(id = "virtual", state = TangemPayCardState.Issuing),
                    deliveringCard(id = "plastic"),
                ),
                expectedBanner = CardsProgressBannerUM.Issuing::class,
            ),
        )

        @JvmStatic
        fun provideCardTileCases() = listOf(
            CardTileCase(
                name = "active card -> digits and artwork shown",
                card = tangemPayCard(images = listOf(thumbnail())),
                expectedLastDigits = "1234",
                expectedImageUrl = THUMBNAIL_URL,
                expectedFrozen = false,
                expectedUiState = TangemPayCardUiState.Active,
            ),
            CardTileCase(
                name = "frozen active card -> digits, artwork and snowflake shown",
                card = tangemPayCard(
                    frozenState = TangemPayCardFrozenState.Frozen,
                    images = listOf(thumbnail()),
                ),
                expectedLastDigits = "1234",
                expectedImageUrl = THUMBNAIL_URL,
                expectedFrozen = true,
                expectedUiState = TangemPayCardUiState.Active,
            ),
            CardTileCase(
                name = "delivering card -> blank placeholder despite backend reporting it frozen",
                card = tangemPayCard(
                    state = TangemPayCardState.Delivering,
                    frozenState = TangemPayCardFrozenState.Frozen,
                    images = listOf(thumbnail()),
                ),
                expectedLastDigits = "",
                expectedImageUrl = null,
                expectedFrozen = false,
                expectedUiState = TangemPayCardUiState.InProgress,
            ),
        )

        private const val THUMBNAIL_URL = "https://tangem.com/card_thumb.png"

        private fun thumbnail() = TangemPayImage(
            type = "THUMBNAIL",
            url = THUMBNAIL_URL,
        )

        private fun deliveringCard(id: String) = tangemPayCard(id = id, state = TangemPayCardState.Delivering)
    }
}