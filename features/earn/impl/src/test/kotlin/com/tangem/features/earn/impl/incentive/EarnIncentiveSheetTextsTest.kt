package com.tangem.features.earn.impl.incentive

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import org.junit.jupiter.api.Test

internal class EarnIncentiveSheetTextsTest {

    @Test
    fun `GIVEN the sheet texts WHEN read THEN they are the agreed copy`() {
        // Assert
        assertThat(EarnIncentiveSheetTexts.title).isEqualTo(stringReference("Incentivised yield"))
        assertThat(EarnIncentiveSheetTexts.poweredByTitle).isEqualTo(stringReference("Powered by Merkl"))
        assertThat(EarnIncentiveSheetTexts.poweredByDescription).isEqualTo(
            stringReference(
                "A campaign sponsor funds extra reward tokens, distributed through Merkl.\n" +
                    "Incentives may end, and reward token prices can change.\n" +
                    "The displayed APY includes incentive rewards after Tangem's 15% service fee.",
            ),
        )
    }
}