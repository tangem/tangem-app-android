package com.tangem.features.tangempay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationPrimaryButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tangempay.components.cardDetails.TangemPayCardDetailsBlockComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM
import com.tangem.features.tangempay.entity.TangemPayEditDisplayNameUM

@Composable
internal fun TangemPayEditDisplayNameScreen(
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

        NavigationPrimaryButton(
            modifier = Modifier
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            primaryButton = NavigationButton(
                textReference = resourceReference(R.string.common_done),
                onClick = state.onDoneClick,
                shouldShowProgress = state.isLoading,
                isEnabled = !state.isLoading && state.editingValue.isNotBlank(),
            ),
        )
    }
}