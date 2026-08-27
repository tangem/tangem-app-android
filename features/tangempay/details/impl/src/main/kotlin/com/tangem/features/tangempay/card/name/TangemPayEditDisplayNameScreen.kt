package com.tangem.features.tangempay.card.name

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cross_20
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.features.tangempay.card.view.TangemPayCard
import com.tangem.features.tangempay.card.view.TangemPayCardDetailsUM
import com.tangem.features.tangempay.details.impl.R

@Composable
internal fun TangemPayEditDisplayNameScreen(
    state: TangemPayEditDisplayNameUM,
    cardDetailsState: TangemPayCardDetailsUM,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary)
            .systemBarsPadding(),
    ) {
        TangemTopBar(
            endContent = {
                TangemButton(
                    modifier = Modifier.testTag(TangemPayTestTags.CARD_RENAME_CLOSE_BUTTON),
                    iconStart = TangemIconUM.Icon(imageVector = Icons.ic_cross_20),
                    onClick = state.onDismiss,
                    size = TangemButton.Size.X11,
                    variant = TangemButton.Variant.Material,
                )
            },
            title = resourceReference(R.string.tangem_pay_rename_card_title),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            TangemPayCard(
                state = cardDetailsState,
                modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x4, vertical = TangemTheme.dimens2.x2),
            )
        }

        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = TangemTheme.dimens2.x3, horizontal = TangemTheme.dimens2.x4)
                .imePadding()
                .testTag(TangemPayTestTags.CARD_RENAME_DONE_BUTTON),
            text = resourceReference(R.string.common_done),
            onClick = state.onDoneClick,
            isLoading = state.isLoading,
            isEnabled = !state.isLoading && state.isDoneEnabled,
            size = TangemButton.Size.X12,
        )
    }
}