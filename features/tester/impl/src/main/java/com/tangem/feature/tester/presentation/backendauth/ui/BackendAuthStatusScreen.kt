package com.tangem.feature.tester.presentation.backendauth.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.divider.DividerWithPadding
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.backendauth.state.BackendAuthStatusUM
import com.tangem.core.ui.R as CoreUiR

/**
 * Read-only screen showing the current Backend Authentication state (toggle, environment,
 * device key, registration flag, session tokens). Keys/tokens are shown shortened with a copy
 * action; all values are also selectable.
 *
 * @param state screen state
 */
@Composable
internal fun BackendAuthStatusScreen(state: BackendAuthStatusUM) {
    Scaffold(
        topBar = {
            AppBarWithBackButton(
                onBackClick = state.onBackClick,
                text = stringResourceSafe(id = R.string.backend_auth_status),
                modifier = Modifier.statusBarsPadding(),
            )
        },
        containerColor = TangemTheme.colors.background.secondary,
    ) { paddingValues ->
        SelectionContainer {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                state.sections.forEachIndexed { sectionIndex, section ->
                    itemsIndexed(items = section.rows, key = { _, row -> "row_${row.label}" }) { index, row ->
                        Column(modifier = Modifier.animateItem()) {
                            StatusRow(
                                row = row,
                                runningAction = state.runningAction,
                                onCopyClick = { state.onCopyClick(row) },
                            )
                            if (index < section.rows.lastIndex) {
                                DividerWithPadding(start = 16.dp, end = 16.dp)
                            }
                        }
                    }

                    items(items = section.actions, key = { "act_${it.label}" }) { action ->
                        SecondaryButton(
                            text = action.label,
                            onClick = action.onClick,
                            showProgress = state.runningAction == action.label,
                            enabled = state.runningAction == null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }

                    if (sectionIndex < state.sections.lastIndex) {
                        item(key = "gap_$sectionIndex") {
                            DividerWithPadding(start = 16.dp, end = 16.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(row: BackendAuthStatusUM.StatusRow, runningAction: String?, onCopyClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text = row.label,
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.secondary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Crossfade(
                targetState = row.value,
                modifier = Modifier.weight(1f),
                label = "value",
            ) { value ->
                Text(
                    text = value,
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.primary1,
                )
            }
            RowTrailingIcons(row = row, runningAction = runningAction, onCopyClick = onCopyClick)
        }
        if (row.subtitle != null) {
            Text(
                text = row.subtitle,
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
    }
}

@Composable
private fun RowTrailingIcons(row: BackendAuthStatusUM.StatusRow, runningAction: String?, onCopyClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (row.copyValue != null) {
            IconButton(
                onClick = onCopyClick,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(CoreUiR.drawable.ic_copy_24),
                    contentDescription = "Copy ${row.label}",
                    tint = TangemTheme.colors.icon.primary1,
                )
            }
        }
        row.iconActions.forEach { iconAction ->
            if (runningAction == iconAction.label && iconAction.isProgressShown) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(32.dp)
                        .padding(6.dp),
                    strokeWidth = 2.dp,
                    color = TangemTheme.colors.icon.primary1,
                )
            } else {
                IconButton(
                    onClick = iconAction.onClick,
                    enabled = runningAction == null,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(iconAction.iconRes),
                        contentDescription = iconAction.label,
                        tint = TangemTheme.colors.icon.primary1,
                    )
                }
            }
        }
    }
}