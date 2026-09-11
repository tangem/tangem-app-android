package com.tangem.features.hotwallet.restorecloudbackup.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_24
import com.tangem.core.ui.res.generated.icons.ic_cloud_24_filled
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.features.hotwallet.common.ui.CloudBackupPasswordField
import com.tangem.features.hotwallet.impl.R
import com.tangem.features.hotwallet.restorecloudbackup.entity.BackupRowUM
import com.tangem.features.hotwallet.restorecloudbackup.entity.RestoreCloudBackupUM
import kotlinx.collections.immutable.persistentListOf
import org.joda.time.DateTime

private val ContentHorizontalPadding = 24.dp
private val ListHorizontalPadding = 16.dp

/** Height the footer overlay takes, reserved at the end of the scrollable content. */
private val FooterHeight = 88.dp

@Composable
private fun footerInsets(): WindowInsets = WindowInsets.ime.union(WindowInsets.navigationBars)

@Composable
internal fun RestoreCloudBackupContent(state: RestoreCloudBackupUM, modifier: Modifier = Modifier) {
    val isEnterPassword = state is RestoreCloudBackupUM.EnterPassword

    TangemTopBarScaffold(
        modifier = modifier,
        topBar = {
            TangemTopNavigation(
                title = if (isEnterPassword) {
                    resourceReference(R.string.hw_cloud_backup_restore_password_navtitle)
                } else {
                    resourceReference(
                        id = R.string.hw_cloud_backup_restore_navtitle_v2,
                        formatArgs = wrappedList(resourceReference(R.string.hw_cloud_backup_service_name)),
                    )
                },
                subtitle = state.accountEmail
                    ?.takeUnless { isEnterPassword }
                    ?.let(::stringReference),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                onBack = state.onBack,
            )
        },
        overlay = { _ ->
            if (state !is RestoreCloudBackupUM.BackupList) {
                Footer(state = state, modifier = Modifier.align(Alignment.BottomCenter))
            }
        },
    ) { contentPadding ->
        when (state) {
            is RestoreCloudBackupUM.BackupList -> BackupListScreen(state, contentPadding)
            is RestoreCloudBackupUM.EnterPassword -> EnterPasswordScreen(state, contentPadding)
            is RestoreCloudBackupUM.EnterPassphrase -> EnterPassphraseScreen(state, contentPadding)
        }
    }
}

@Composable
private fun Footer(state: RestoreCloudBackupUM, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(footerInsets()),
    ) {
        TangemFade(
            modifier = Modifier.matchParentSize(),
            position = TangemFade.Position.Bottom,
        )
        when (state) {
            is RestoreCloudBackupUM.EnterPassword -> FooterButton(
                text = stringResourceSafe(R.string.hw_cloud_backup_restore_password_button),
                isEnabled = state.isRestoreEnabled && !state.isLoading,
                isLoading = state.isLoading,
                onClick = state.onRestoreClick,
            )
            is RestoreCloudBackupUM.EnterPassphrase -> FooterButton(
                text = stringResourceSafe(R.string.common_continue),
                isEnabled = state.isContinueEnabled && !state.isLoading,
                isLoading = state.isLoading,
                onClick = state.onContinueClick,
            )
            is RestoreCloudBackupUM.BackupList -> Unit
        }
    }
}

@Composable
private fun FooterButton(text: String, isEnabled: Boolean, isLoading: Boolean, onClick: () -> Unit) {
    TangemButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        size = TangemButton.Size.X12,
        text = stringReference(text),
        isEnabled = isEnabled,
        isLoading = isLoading,
        onClick = onClick,
    )
}

@Composable
private fun EnterPasswordScreen(
    state: RestoreCloudBackupUM.EnterPassword,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val createdAt = DateTimeFormatters.formatDate(
        date = DateTime(state.createdAtMillis),
        formatter = DateTimeFormatters.dateTimeMMMdYYYY,
    )
    ScrollableContent(contentPadding = contentPadding, modifier = modifier) {
        TitleBlock(
            title = stringResourceSafe(R.string.hw_cloud_backup_restore_password_title),
            description = stringResourceSafe(
                R.string.hw_cloud_backup_restore_password_description,
                state.walletName,
                stringResourceSafe(R.string.hw_cloud_backup_service_name),
                createdAt,
            ),
        )
        SpacerH(24.dp)
        CloudBackupPasswordField(
            value = state.password,
            onValueChange = state.onPasswordChange,
            isVisible = state.isPasswordVisible,
            onToggleVisibility = state.onToggleVisibility,
            errorText = resourceReference(R.string.hw_cloud_backup_restore_wrong_password)
                .takeIf { state.isError },
            enabled = !state.isLoading,
            focusRequester = focusRequester,
            contentType = ContentType.Password,
        )
    }
}

@Composable
private fun EnterPassphraseScreen(
    state: RestoreCloudBackupUM.EnterPassphrase,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ScrollableContent(contentPadding = contentPadding, modifier = modifier) {
        TitleBlock(
            title = stringResourceSafe(R.string.hw_cloud_backup_restore_passphrase_title),
            description = stringResourceSafe(R.string.hw_cloud_backup_restore_passphrase_description),
        )
        SpacerH(24.dp)
        CloudBackupPasswordField(
            value = state.passphrase,
            onValueChange = state.onPassphraseChange,
            isVisible = state.isPassphraseVisible,
            onToggleVisibility = state.onToggleVisibility,
            label = resourceReference(R.string.common_passphrase),
            enabled = !state.isLoading,
            focusRequester = focusRequester,
        )
    }
}

@Composable
private fun BackupListScreen(
    state: RestoreCloudBackupUM.BackupList,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = contentPadding.calculateTopPadding())
            .padding(horizontal = ListHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SpacerH(12.dp)
        state.items.forEach { row -> BackupRow(row) }
        SpacerH(contentPadding.calculateBottomPadding())
    }
}

@Composable
private fun BackupRow(row: BackupRowUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors3.bg.secondary)
            .clickableSingle(onClick = row.onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(TangemTheme.colors3.bg.status.infoSubtle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.ic_cloud_24_filled,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.status.info,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.walletName,
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = DateTimeFormatters.formatDate(
                    date = DateTime(row.createdAtMillis),
                    formatter = DateTimeFormatters.dateTimeMMMdYYYY,
                ),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.ic_chevron_right_24,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.secondary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun ScrollableContent(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    footerHeight: Dp = FooterHeight,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(footerInsets())
            .verticalScroll(rememberScrollState())
            .padding(top = contentPadding.calculateTopPadding())
            .padding(horizontal = ContentHorizontalPadding),
    ) {
        SpacerH(20.dp)
        content()
        SpacerH(footerHeight)
    }
}

@Composable
private fun TitleBlock(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
        )
        SpacerH(8.dp)
        Text(
            text = description,
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Preview(showBackground = true, widthDp = 360, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewEnterPassword() {
    TangemThemePreviewRedesign {
        RestoreCloudBackupContent(
            state = RestoreCloudBackupUM.EnterPassword(
                walletName = "My Wallet",
                createdAtMillis = 0L,
                password = "secret",
                isPasswordVisible = false,
                isError = true,
                isLoading = false,
                onPasswordChange = {},
                onToggleVisibility = {},
                onRestoreClick = {},
                accountEmail = "user@gmail.com",
                onBack = {},
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Preview(showBackground = true, widthDp = 360, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewBackupList() {
    TangemThemePreviewRedesign {
        RestoreCloudBackupContent(
            state = RestoreCloudBackupUM.BackupList(
                items = persistentListOf(
                    BackupRowUM(walletName = "My Wallet", createdAtMillis = 0L, onClick = {}),
                    BackupRowUM(walletName = "Savings", createdAtMillis = 1_000L, onClick = {}),
                ),
                accountEmail = "user@gmail.com",
                onBack = {},
            ),
        )
    }
}