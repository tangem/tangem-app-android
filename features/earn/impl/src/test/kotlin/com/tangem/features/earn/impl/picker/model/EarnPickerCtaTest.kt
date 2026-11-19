package com.tangem.features.earn.impl.picker.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.earn.impl.R
import com.tangem.features.earn.impl.fake.FakeEarnPosition
import com.tangem.features.earn.impl.fake.FakeEarnProductPreset
import com.tangem.features.earn.impl.fake.fakeReadyState
import com.tangem.features.earn.product.api.product.EarnBlock
import com.tangem.features.earn.product.api.product.EarnFreshness
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.session.EarnAction
import com.tangem.test.core.ProvideTestModels
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnPickerCtaTest {

    private val cta = EarnPickerCta()

    @ParameterizedTest
    @ProvideTestModels
    fun resolve(model: ResolveModel) {
        // Arrange
        val calls = mutableListOf<Call>()

        // Act
        val result = cta.resolve(
            state = model.state,
            ethBalance = model.ethBalance,
            onActionClick = { calls += Call.Action(it) },
            onBlocked = { calls += Call.Blocked(it) },
            onAddFunds = { calls += Call.AddFunds },
        )
        result.onClick()

        // Assert
        val expected = TangemButtonUM(
            text = model.text,
            isEnabled = model.isEnabled,
            isDimmed = model.isDimmed,
            type = TangemButtonType.Primary,
            onClick = NoOp,
        )
        assertThat(result.copy(onClick = NoOp)).isEqualTo(expected)
        assertThat(calls).containsExactlyElementsIn(model.expectedCalls).inOrder()
    }

    private fun provideTestModels(): List<ResolveModel> {
        val ready = fakeReadyState(FakeEarnProductPreset.Lending, FakeEarnPosition.None)
        val label = ready.primary.label
        val action = ready.primary.action
        val minDeposit = ready.limits.minDeposit
        val productBlocked = ready.copy(primary = ready.primary.copy(block = PRODUCT_BLOCK))
        val stale = ready.copy(freshness = STALE)
        val staleAndProductBlocked = productBlocked.copy(freshness = STALE)
        val staked = fakeReadyState(FakeEarnProductPreset.Lending, FakeEarnPosition.Known)
        val noMinimum = ready.copy(limits = ready.limits.copy(minDeposit = BigDecimal.ZERO))

        return listOf(
            idle(state = EarnProductState.Loading),
            idle(state = EarnProductState.Unavailable),
            ResolveModel(
                state = ready,
                ethBalance = null,
                text = label,
                isEnabled = false,
                isDimmed = false,
            ),
            blocked(state = productBlocked, ethBalance = BigDecimal.ONE, block = PRODUCT_BLOCK, label = label),
            blocked(state = stale, ethBalance = BigDecimal.ONE, block = OUT_OF_DATE_BLOCK, label = label),
            blocked(state = staleAndProductBlocked, ethBalance = BigDecimal.ONE, block = OUT_OF_DATE_BLOCK, label = label),
            blocked(state = productBlocked, ethBalance = BigDecimal.ZERO, block = PRODUCT_BLOCK, label = label),
            addFunds(state = ready, ethBalance = BigDecimal.ZERO),
            addFunds(state = ready, ethBalance = BigDecimal("0.005")),
            addFunds(state = noMinimum, ethBalance = BigDecimal.ZERO),
            start(state = noMinimum, ethBalance = BigDecimal("0.001"), label = label, action = action),
            start(state = ready, ethBalance = minDeposit, label = label, action = action),
            start(state = ready, ethBalance = BigDecimal("5"), label = label, action = action),
            start(
                state = staked,
                ethBalance = BigDecimal.ZERO,
                label = staked.primary.label,
                action = staked.primary.action,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class KindOf {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN product state and balance WHEN kind THEN button kind`(model: KindModel) {
            // Act
            val actual = cta.kind(state = model.state, ethBalance = model.ethBalance)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<KindModel> {
            val ready = fakeReadyState(FakeEarnProductPreset.Lending, FakeEarnPosition.None)
            val minDeposit = ready.limits.minDeposit
            val productBlocked = ready.copy(primary = ready.primary.copy(block = PRODUCT_BLOCK))
            val stale = ready.copy(freshness = STALE)
            val staked = fakeReadyState(FakeEarnProductPreset.Lending, FakeEarnPosition.Known)

            return listOf(
                KindModel(EarnProductState.Loading, BigDecimal.ONE, EarnPickerCta.Kind.Disabled),
                KindModel(EarnProductState.Unavailable, BigDecimal.ONE, EarnPickerCta.Kind.Disabled),
                KindModel(ready, null, EarnPickerCta.Kind.Disabled),
                KindModel(productBlocked, BigDecimal.ONE, EarnPickerCta.Kind.Blocked),
                KindModel(productBlocked, BigDecimal.ZERO, EarnPickerCta.Kind.Blocked),
                KindModel(stale, BigDecimal.ONE, EarnPickerCta.Kind.Blocked),
                KindModel(ready, BigDecimal.ZERO, EarnPickerCta.Kind.AddFunds),
                KindModel(ready, BigDecimal("0.005"), EarnPickerCta.Kind.AddFunds),
                KindModel(ready, minDeposit, EarnPickerCta.Kind.Primary),
                KindModel(ready, BigDecimal("5"), EarnPickerCta.Kind.Primary),
                KindModel(staked, BigDecimal.ZERO, EarnPickerCta.Kind.Primary),
            )
        }
    }

    private fun idle(state: EarnProductState) = ResolveModel(
        state = state,
        ethBalance = BigDecimal.ONE,
        text = resourceReference(R.string.eth_yield_start_earning),
        isEnabled = false,
        isDimmed = false,
    )

    private fun blocked(state: EarnProductState, ethBalance: BigDecimal, block: EarnBlock, label: TextReference) =
        ResolveModel(
            state = state,
            ethBalance = ethBalance,
            text = label,
            isEnabled = true,
            isDimmed = true,
            expectedCalls = listOf(Call.Blocked(block)),
        )

    private fun addFunds(state: EarnProductState, ethBalance: BigDecimal) = ResolveModel(
        state = state,
        ethBalance = ethBalance,
        text = resourceReference(R.string.common_add_funds),
        isEnabled = true,
        isDimmed = false,
        expectedCalls = listOf(Call.AddFunds),
    )

    private fun start(state: EarnProductState, ethBalance: BigDecimal, label: TextReference, action: EarnAction) =
        ResolveModel(
            state = state,
            ethBalance = ethBalance,
            text = label,
            isEnabled = true,
            isDimmed = false,
            expectedCalls = listOf(Call.Action(action)),
        )

    internal data class ResolveModel(
        val state: EarnProductState,
        val ethBalance: BigDecimal?,
        val text: TextReference,
        val isEnabled: Boolean,
        val isDimmed: Boolean,
        val expectedCalls: List<Call> = emptyList(),
    )

    internal data class KindModel(
        val state: EarnProductState,
        val ethBalance: BigDecimal?,
        val expected: EarnPickerCta.Kind,
    )

    internal sealed interface Call {
        data class Action(val action: EarnAction) : Call
        data class Blocked(val block: EarnBlock) : Call
        data object AddFunds : Call
    }

    private companion object {

        val NoOp: () -> Unit = {}
        val STALE = EarnFreshness(asOf = Instant.fromEpochSeconds(0), isStale = true)
        val PRODUCT_BLOCK = EarnBlock(reason = stringReference("blocked"))
        val OUT_OF_DATE_BLOCK = EarnBlock(
            reason = resourceReference(R.string.token_button_unavailability_reason_out_of_date_balance),
        )
    }
}