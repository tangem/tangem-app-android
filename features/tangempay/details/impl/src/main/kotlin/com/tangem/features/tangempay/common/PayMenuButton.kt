package com.tangem.features.tangempay.common

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.tangem.core.ui.ds2.button.GroupEntry
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.menu.TangemMenu
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_dots_vertical_20

@Composable
internal fun PayMenuButton(
    menu: TangemMenu.UiState,
    testTag: String,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit = {},
) {
    var isMenuExpanded by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier) {
        TangemButton.GroupEntry(
            modifier = Modifier.testTag(testTag),
            imageVector = Icons.ic_dots_vertical_20,
            onClick = {
                onOpen()
                isMenuExpanded = true
            },
        )
        TangemMenu(
            state = menu,
            isExpanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
        )
    }
}