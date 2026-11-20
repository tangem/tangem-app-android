package com.tangem.features.allservices.impl.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.GroupEntry
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_gift_24

@Composable
internal fun AllServicesScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemTopBarScaffold(
        modifier = modifier,
        topBar = {
            TangemTopNavigation(
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                isEndButtonsGroupBackgroundShown = true,
                endButtonsGroup = {
                    TangemButton.GroupEntry(
                        iconUM = TangemIconUM.Icon(imageVector = Icons.ic_gift_24),
                        onClick = onBackClick,
                    )
                },
                endButton = {
                    TangemButton.Close(onClick = onBackClick)
                },
            )
        },
        content = {},
    )
}

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun AllServicesScreen_Preview() {
    TangemThemePreviewRedesign {
        AllServicesScreen(onBackClick = {})
    }
}