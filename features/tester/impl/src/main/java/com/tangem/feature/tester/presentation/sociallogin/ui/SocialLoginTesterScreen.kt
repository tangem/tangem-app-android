package com.tangem.feature.tester.presentation.sociallogin.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.divider.DividerWithPadding
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.sociallogin.state.SocialLoginTesterUM

@Composable
internal fun SocialLoginTesterScreen(state: SocialLoginTesterUM) {
    Scaffold(
        topBar = {
            AppBarWithBackButton(
                onBackClick = state.onBackClick,
                text = stringResourceSafe(id = R.string.social_login),
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
                items(items = state.rows, key = { "row_${it.label}" }) { row ->
                    StatusRow(row)
                }

                item(key = "divider") { DividerWithPadding(start = 16.dp, end = 16.dp) }

                items(items = state.actions, key = { "act_${it.label}" }) { action ->
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
            }
        }
    }
}

@Composable
private fun StatusRow(row: SocialLoginTesterUM.StatusRow) {
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
        Text(
            text = row.value,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
    }
}