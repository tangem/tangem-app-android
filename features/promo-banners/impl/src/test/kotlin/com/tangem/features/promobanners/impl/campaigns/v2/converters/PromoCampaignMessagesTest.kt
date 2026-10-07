package com.tangem.features.promobanners.impl.campaigns.v2.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.v2.createPromoCampaign
import org.junit.jupiter.api.Test

internal class PromoCampaignMessagesTest {

    @Test
    fun `GIVEN campaign WHEN alreadyEnrolled THEN title carries localized campaign name`() {
        // Act
        val actual = PromoCampaignMessages.alreadyEnrolled(createPromoCampaign())

        // Assert
        val title = actual.elements.filterIsInstance<MessageBottomSheetUM.InfoBlock>().single().title
        assertThat(title).isEqualTo(
            resourceReference(R.string.promo_campaign_already_activated_title, wrappedList("Whale cashback")),
        )
    }
}