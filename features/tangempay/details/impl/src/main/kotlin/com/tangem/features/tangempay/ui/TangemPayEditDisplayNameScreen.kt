package com.tangem.features.tangempay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cross_20
import com.tangem.features.tangempay.components.cardDetails.TangemPayCardDetailsBlockComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM
import com.tangem.features.tangempay.entity.TangemPayEditDisplayNameUM

@Composable
internal fun TangemPayEditDisplayNameScreen(
    isRedesignEnabled: Boolean,
    state: TangemPayEditDisplayNameUM,
    cardDetailsBlockComponent: TangemPayCardDetailsBlockComponent,
    cardDetailsState: TangemPayCardDetailsUM,
    modifier: Modifier = Modifier,
) {
    if (isRedesignEnabled) {
        TangemPayEditDisplayNameScreenV2(
            state = state,
            cardDetailsBlockComponent = cardDetailsBlockComponent,
            cardDetailsState = cardDetailsState,
            modifier = modifier,
        )
    } else {
        TangemPayEditDisplayNameScreenV1(
            state = state,
            cardDetailsBlockComponent = cardDetailsBlockComponent,
            cardDetailsState = cardDetailsState,
            modifier = modifier,
        )
    }
}

@Composable
internal fun TangemPayEditDisplayNameScreenV1(
    state: TangemPayEditDisplayNameUM,
    cardDetailsBlockComponent: TangemPayCardDetailsBlockComponent,
    cardDetailsState: TangemPayCardDetailsUM,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.secondary)
            .navigationBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .height(56.dp)
                .fillMaxWidth(),
        ) {
            IconButton(
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                onClick = state.onDismiss,
            ) {
                Icon(
                    painter = painterResource(id = com.tangem.core.ui.R.drawable.ic_close_24),
                    contentDescription = null,
                    tint = TangemTheme.colors.icon.primary1,
                )
            }
        }

        cardDetailsBlockComponent.CardDetailsBlockContent(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
            state = cardDetailsState,
        )

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            modifier = Modifier
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            text = stringResourceSafe(R.string.common_done),
            onClick = state.onDoneClick,
            showProgress = state.isLoading,
            enabled = !state.isLoading && state.isDoneEnabled,
        )
    }
}

@Composable
internal fun TangemPayEditDisplayNameScreenV2(
    state: TangemPayEditDisplayNameUM,
    cardDetailsBlockComponent: TangemPayCardDetailsBlockComponent,
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
            cardDetailsBlockComponent.CardDetailsBlockContent(
                modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x4, vertical = TangemTheme.dimens2.x2),
                state = cardDetailsState,
            )
        }

        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = TangemTheme.dimens2.x3, horizontal = TangemTheme.dimens2.x4)
                .imePadding(),
            text = resourceReference(R.string.common_done),
            onClick = state.onDoneClick,
            isLoading = state.isLoading,
            isEnabled = !state.isLoading && state.isDoneEnabled,
            size = TangemButton.Size.X12,
        )
    }
}