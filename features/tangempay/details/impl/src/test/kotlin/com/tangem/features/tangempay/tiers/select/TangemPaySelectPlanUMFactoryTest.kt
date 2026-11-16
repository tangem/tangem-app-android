package com.tangem.features.tangempay.tiers.select

import android.text.format.DateFormat
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.features.tangempay.components.TangemPaySelectPlanComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.joda.time.DateTime
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class TangemPaySelectPlanUMFactoryTest {

    private val clickIntents: TangemPaySelectPlanClickIntents = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(clickIntents)
    }

    private fun createFactory(
        mode: TangemPaySelectPlanComponent.Mode = TangemPaySelectPlanComponent.Mode.ChangePlan,
    ) = TangemPaySelectPlanUMFactory(mode = mode, clicks = clickIntents)

    @Nested
    inner class Plans {

        @Test
        fun `GIVEN mixed transitions WHEN create THEN only allowed types mapped to plans with onboarding points`() {
            // Arrange
            val plan = plan(
                tierId = "PLUS",
                descriptionItems = listOf(
                    item(TangemPayTariffPlan.Section.ONBOARDING_RELATED, order = 2, title = "Second", body = ""),
                    item(TangemPayTariffPlan.Section.CARD_RELATED, order = 0, title = "Card", body = "card"),
                    item(TangemPayTariffPlan.Section.ONBOARDING_RELATED, order = 1, title = "First", body = "body"),
                ),
            )
            val data = data(
                transitions = listOf(
                    TangemPayTariffPlanTransition(TangemPayTariffPlanTransition.Type.UPGRADE, plan),
                    transition(TangemPayTariffPlanTransition.Type.SYSTEM_DOWNGRADE, "BASIC"),
                    transition(TangemPayTariffPlanTransition.Type.UNKNOWN, "BASIC"),
                ),
            )

            // Act
            val actual = createFactory().create(data).plans

            // Assert
            assertThat(actual).containsExactly(
                TangemPaySelectPlanUM.PlanUM(
                    name = stringReference("PLUS"),
                    imageUrl = null,
                    points = persistentListOf(
                        TangemPaySelectPlanUM.PointUM(title = stringReference("First"), body = stringReference("body")),
                        TangemPaySelectPlanUM.PointUM(title = stringReference("Second"), body = null),
                    ),
                ),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SelectContent {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN mode WHEN create THEN select button icon matches money account activation`(model: IconModel) {
            // Act
            val content = createFactory(mode = model.mode).create(data()).content as TangemPaySelectPlanUM.Content.Select

            // Assert
            assertThat(content.selectButtonIconEnd != null).isEqualTo(model.hasIcon)
        }

        @Test
        fun `GIVEN confirm without selected transition WHEN create THEN select content shown`() {
            // Arrange
            val data = data(transitions = emptyList(), isConfirm = true)

            // Act
            val actual = createFactory().create(data)

            // Assert
            assertThat(actual.content).isInstanceOf(TangemPaySelectPlanUM.Content.Select::class.java)
            assertThat(actual.topBarTitle).isEqualTo(resourceReference(R.string.tangempay_select_plan_confirm_title))
        }

        private fun provideTestModels() = listOf(
            IconModel(mode = TangemPaySelectPlanComponent.Mode.ChangePlan, hasIcon = false),
            IconModel(mode = TangemPaySelectPlanComponent.Mode.TiersOnboarding, hasIcon = false),
            IconModel(
                mode = TangemPaySelectPlanComponent.Mode.CreateOnboardingOrder(
                    availableProducts = setOf(TangemPayProduct.PAYMENT_ACCOUNT),
                ),
                hasIcon = false,
            ),
            IconModel(
                mode = TangemPaySelectPlanComponent.Mode.CreateOnboardingOrder(
                    availableProducts = setOf(TangemPayProduct.BANKING_ACCOUNT, TangemPayProduct.PAYMENT_ACCOUNT),
                ),
                hasIcon = true,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConfirmContent {

        @BeforeAll
        fun mockDateFormat() {
            mockkStatic(DateFormat::class)
            every { DateFormat.getBestDateTimePattern(any(), any()) } returns "MMM d"
        }

        @AfterAll
        fun unmockDateFormat() {
            unmockkStatic(DateFormat::class)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN selected transition WHEN create in confirm THEN confirm content built`(model: ConfirmModel) {
            // Arrange
            val data = data(
                transitions = listOf(model.transition),
                tariffPlan = model.tariffPlan,
                isConfirm = true,
            )

            // Act
            val actual = createFactory().create(data).content

            // Assert
            assertThat(actual).isEqualTo(
                TangemPaySelectPlanUM.Content.Confirm(
                    title = model.expectedTitle,
                    points = model.expectedPoints
                        .map { TangemPaySelectPlanUM.PointUM(title = it, body = null) }
                        .let { persistentListOf(*it.toTypedArray()) },
                    confirmButtonText = model.expectedButtonText,
                    isProcessing = false,
                    onCancelClick = clickIntents::onBackClick,
                    onConfirmClick = clickIntents::onConfirmClick,
                ),
            )
        }

        private fun provideTestModels(): List<ConfirmModel> {
            val currentPlan = plan(tierId = "PLUS")
            val upgrade = transition(TangemPayTariffPlanTransition.Type.UPGRADE, "PRO")
            val downgrade = transition(TangemPayTariffPlanTransition.Type.DOWNGRADE, "BASIC")
            val activation = transition(TangemPayTariffPlanTransition.Type.ACTIVATION, "PLUS")
            val billingDate = DateTime(2026, 12, 15, 0, 0)
            val formattedDate = DateTimeFormatters.formatDate(billingDate, DateTimeFormatters.dateMMMd)

            return listOf(
                ConfirmModel(
                    transition = upgrade,
                    tariffPlan = customerTariff(currentPlan, nextBillingAt = null),
                    expectedTitle = resourceReference(
                        R.string.tangempay_select_plan_confirm_upgrade_title,
                        wrappedList("program-PRO"),
                    ),
                    expectedPoints = listOf(
                        resourceReference(
                            R.string.tangempay_select_plan_confirm_point_virtual_card,
                            wrappedList("program-PRO"),
                        ),
                    ),
                    expectedButtonText = resourceReference(R.string.tangempay_select_plan_btn_upgrade),
                ),
                ConfirmModel(
                    transition = downgrade,
                    tariffPlan = customerTariff(currentPlan, nextBillingAt = billingDate),
                    expectedTitle = resourceReference(
                        R.string.tangempay_select_plan_confirm_downgrade_title,
                        wrappedList("PLUS", "program-PLUS", formattedDate),
                    ),
                    expectedPoints = listOf(
                        resourceReference(
                            R.string.tangempay_select_plan_confirm_point_move_on_date,
                            wrappedList(formattedDate, "BASIC"),
                        ),
                        resourceReference(
                            R.string.tangempay_select_plan_confirm_point_cards_closed,
                            wrappedList("program-PLUS"),
                        ),
                        resourceReference(
                            R.string.tangempay_select_plan_confirm_point_cancel_till,
                            wrappedList(formattedDate),
                        ),
                        resourceReference(R.string.tangempay_select_plan_confirm_point_no_fee),
                    ),
                    expectedButtonText = resourceReference(R.string.tangempay_select_plan_btn_downgrade),
                ),
                ConfirmModel(
                    transition = downgrade,
                    tariffPlan = null,
                    expectedTitle = resourceReference(
                        R.string.tangempay_select_plan_confirm_switch_title,
                        wrappedList("BASIC"),
                    ),
                    expectedPoints = listOf(resourceReference(R.string.tangempay_select_plan_confirm_point_no_fee)),
                    expectedButtonText = resourceReference(R.string.tangempay_select_plan_btn_downgrade),
                ),
                ConfirmModel(
                    transition = activation,
                    tariffPlan = customerTariff(currentPlan, nextBillingAt = billingDate),
                    expectedTitle = resourceReference(
                        R.string.tangempay_select_plan_confirm_switch_title,
                        wrappedList("PLUS"),
                    ),
                    expectedPoints = listOf(resourceReference(R.string.tangempay_select_plan_confirm_point_no_fee)),
                    expectedButtonText = resourceReference(R.string.common_continue),
                ),
            )
        }
    }

    @Nested
    inner class Compare {

        @Test
        fun `GIVEN compare hidden WHEN create THEN compare is null`() {
            // Act
            val actual = createFactory().create(data(isCompareShown = false))

            // Assert
            assertThat(actual.compare).isNull()
        }

        @Test
        fun `GIVEN compare shown WHEN create THEN current plan first and attributes ordered by section and order`() {
            // Arrange
            val currentPlan = plan(
                tierId = "PLUS",
                descriptionItems = listOf(
                    item(TangemPayTariffPlan.Section.PLAN_RELATED, order = 0, title = "Fee", body = "5"),
                    item(TangemPayTariffPlan.Section.CARD_RELATED, order = 1, title = "Cards", body = "2"),
                    item(TangemPayTariffPlan.Section.ONBOARDING_RELATED, order = 0, title = "Skip", body = "x"),
                ),
            )
            val upgradePlan = plan(
                tierId = "PRO",
                descriptionItems = listOf(
                    item(TangemPayTariffPlan.Section.CARD_RELATED, order = 0, title = "Limit", body = "10k"),
                    item(TangemPayTariffPlan.Section.CARD_RELATED, order = 1, title = "Cards", body = "5"),
                ),
            )
            val data = data(
                transitions = listOf(
                    TangemPayTariffPlanTransition(TangemPayTariffPlanTransition.Type.UPGRADE, upgradePlan),
                    transition(TangemPayTariffPlanTransition.Type.ACTIVATION, "BASIC"),
                ),
                tariffPlan = customerTariff(currentPlan, nextBillingAt = null),
                isCompareShown = true,
            )

            // Act
            val actual = createFactory().create(data).compare

            // Assert
            assertThat(actual).isEqualTo(
                TangemPaySelectPlanUM.ComparePlans(
                    attributes = persistentListOf(
                        stringReference("Limit"),
                        stringReference("Cards"),
                        stringReference("Fee"),
                    ),
                    plans = persistentListOf(
                        TangemPaySelectPlanUM.ComparePlans.Plan(
                            name = stringReference("PLUS"),
                            values = persistentListOf(stringReference(""), stringReference("2"), stringReference("5")),
                        ),
                        TangemPaySelectPlanUM.ComparePlans.Plan(
                            name = stringReference("PRO"),
                            values = persistentListOf(stringReference("10k"), stringReference("5"), stringReference("")),
                        ),
                    ),
                    onDismiss = clickIntents::onCompareDismiss,
                ),
            )
        }
    }

    @Nested
    inner class LoadingAndError {

        @Test
        fun `GIVEN initial data WHEN createLoading THEN loading content without plans`() {
            // Act
            val actual = createFactory().createLoading(TangemPaySelectPlanData())

            // Assert
            assertThat(actual.content).isEqualTo(TangemPaySelectPlanUM.Content.Loading)
            assertThat(actual.plans).isEmpty()
        }

        @Test
        fun `GIVEN error content WHEN onRetryClick THEN click intent invoked`() {
            // Arrange
            val content = createFactory().createError(TangemPaySelectPlanData()).content
                as TangemPaySelectPlanUM.Content.Error

            // Act
            content.onRetryClick()

            // Assert
            verify(exactly = 1) { clickIntents.onRetryClick() }
        }
    }

    data class IconModel(
        val mode: TangemPaySelectPlanComponent.Mode,
        val hasIcon: Boolean,
    )

    data class ConfirmModel(
        val transition: TangemPayTariffPlanTransition,
        val tariffPlan: TangemPayCustomerTariffPlan?,
        val expectedTitle: TextReference,
        val expectedPoints: List<TextReference>,
        val expectedButtonText: TextReference,
    )

    private companion object {

        fun data(
            transitions: List<TangemPayTariffPlanTransition> = listOf(
                transition(TangemPayTariffPlanTransition.Type.UPGRADE, "PRO"),
            ),
            tariffPlan: TangemPayCustomerTariffPlan? = null,
            isConfirm: Boolean = false,
            isCompareShown: Boolean = false,
        ) = TangemPaySelectPlanData().copy(
            transitions = transitions,
            tariffPlan = tariffPlan,
            isConfirm = isConfirm,
            isCompareShown = isCompareShown,
        )

        fun transition(type: TangemPayTariffPlanTransition.Type, tierId: String) =
            TangemPayTariffPlanTransition(type = type, plan = plan(tierId))

        fun plan(
            tierId: String,
            descriptionItems: List<TangemPayTariffPlan.DescriptionItem> = emptyList(),
        ) = TangemPayTariffPlan(
            id = "plan-${tierId.lowercase()}",
            tierId = tierId,
            isBasicTier = false,
            name = tierId,
            programName = "program-$tierId",
            descriptionItems = descriptionItems,
        )

        fun item(section: TangemPayTariffPlan.Section, order: Int, title: String, body: String) =
            TangemPayTariffPlan.DescriptionItem(section = section, order = order, title = title, body = body)

        fun customerTariff(plan: TangemPayTariffPlan, nextBillingAt: DateTime?) = TangemPayCustomerTariffPlan(
            status = TangemPayCustomerTariffPlan.Status.ACTIVE,
            source = TangemPayCustomerTariffPlan.Source.CUSTOMER,
            plan = plan,
            nextBillingAt = nextBillingAt,
            pendingPlan = null,
            pendingTransitionAt = null,
        )
    }
}