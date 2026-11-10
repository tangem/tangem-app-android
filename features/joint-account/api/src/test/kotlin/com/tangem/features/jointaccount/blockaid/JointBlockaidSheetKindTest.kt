package com.tangem.features.jointaccount.blockaid

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.blockaid.JointOperationBlockaidGate.Verdict
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointBlockaidSheetKindTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a verdict WHEN of THEN the sheet it requires`(model: KindModel) {
        // Act
        val actual = JointBlockaidSheetKind.of(model.verdict)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class KindModel(val verdict: Verdict, val expected: JointBlockaidSheetKind?)

    private fun provideTestModels() = listOf(
        KindModel(verdict = Verdict.Warning, expected = JointBlockaidSheetKind.RISKY),
        KindModel(verdict = Verdict.Malicious, expected = JointBlockaidSheetKind.MALICIOUS),
        KindModel(verdict = Verdict.Safe, expected = null),
        KindModel(verdict = Verdict.NoVerdict(Verdict.NoVerdict.Reason.ERROR), expected = null),
        KindModel(verdict = Verdict.NoVerdict(Verdict.NoVerdict.Reason.TIMEOUT), expected = null),
        KindModel(verdict = Verdict.NoVerdict(Verdict.NoVerdict.Reason.UNSUPPORTED_NETWORK), expected = null),
    )
}