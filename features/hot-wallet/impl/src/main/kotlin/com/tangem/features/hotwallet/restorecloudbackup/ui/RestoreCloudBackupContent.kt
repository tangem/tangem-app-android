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
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_24
import com.tangem.core.ui.res.generated.icons.ic_cloud_24_filled
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.features.hotwallet.common.ui.CloudBackupPasswordField
import com.tangem.features.hotwallet.impl.R
import com.tangem.features.hotwallet.restorecloudbackup.entity.BackupRowUM
import kotlinx.collections.immutable.persistentListOf
import com.tangem.features.hotwallet.restorecloudbackup.entity.RestoreCloudBackupUM
import org.joda.time.DateTime

@Composable
internal fun RestoreCloudBackupContent(state: RestoreCloudBackupUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color = TangemTheme.colors3.bg.primary)
            .fillMaxSize()
            .imePadding(),
    ) {
        val isEnterPassword = state is RestoreCloudBackupUM.EnterPassword
        TangemTopAppBar(
            title = if (isEnterPassword) {
                stringResourceSafe(R.string.hw_cloud_backup_restore_password_navtitle)
            } else {
                stringResourceSafe(
                    R.string.hw_cloud_backup_restore_navtitle_v2,
                    stringResourceSafe(R.string.hw_cloud_backup_service_name),
                )
            },
            subtitle = if (isEnterPassword) null else state.accountEmail,
            titleAlignment = Alignment.CenterHorizontally,
            startButton = TopAppBarButtonUM.Back(onBackClicked = state.onBack),
        )

        when (state) {
            is RestoreCloudBackupUM.BackupList -> BackupListScreen(state, Modifier.weight(1f))
            is RestoreCloudBackupUM.EnterPassword -> EnterPasswordScreen(state, Modifier.weight(1f))
            is RestoreCloudBackupUM.EnterPassphrase -> EnterPassphraseScreen(state, Modifier.weight(1f))
        }
    }
}

@Composable
private fun EnterPasswordScreen(state: RestoreCloudBackupUM.EnterPassword, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val createdAt = DateTimeFormatters.formatDate(
        date = DateTime(state.createdAtMillis),
        formatter = DateTimeFormatters.dateTimeMMMdYYYY,
    )
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TitleBlock(
                title = stringResourceSafe(R.string.hw_cloud_backup_restore_password_title),
                description = stringResourceSafe(
                    R.string.hw_cloud_backup_restore_password_description,
                    state.walletName,
                    stringResourceSafe(R.string.hw_cloud_backup_service_name),
                    createdAt,
                ),
                modifier = Modifier.padding(top = 20.dp),
            )
            CloudBackupPasswordField(
                value = state.password,
                onValueChange = state.onPasswordChange,
                isVisible = state.isPasswordVisible,
                onToggleVisibility = state.onToggleVisibility,
                isError = state.isError,
                enabled = !state.isLoading,
                focusRequester = focusRequester,
                contentType = ContentType.Password,
            )
            if (state.isError) {
                Text(
                    text = stringResourceSafe(R.string.hw_cloud_backup_restore_wrong_password),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors3.text.status.error,
                )
            }
        }
        PrimaryButton(
            text = stringResourceSafe(R.string.hw_cloud_backup_restore_password_button),
            onClick = state.onRestoreClick,
            enabled = state.isRestoreEnabled && !state.isLoading,
            showProgress = state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        )
    }
}

@Composable
private fun EnterPassphraseScreen(state: RestoreCloudBackupUM.EnterPassphrase, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TitleBlock(
                title = stringResourceSafe(R.string.hw_cloud_backup_restore_passphrase_title),
                description = stringResourceSafe(R.string.hw_cloud_backup_restore_passphrase_description),
                modifier = Modifier.padding(top = 20.dp),
            )
            CloudBackupPasswordField(
                value = state.passphrase,
                onValueChange = state.onPassphraseChange,
                isVisible = state.isPassphraseVisible,
                onToggleVisibility = state.onToggleVisibility,
                isError = false,
                enabled = !state.isLoading,
                focusRequester = focusRequester,
                placeholder = resourceReference(R.string.common_passphrase),
            )
        }
        PrimaryButton(
            text = stringResourceSafe(R.string.common_continue),
            onClick = state.onContinueClick,
            enabled = state.isContinueEnabled && !state.isLoading,
            showProgress = state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        )
    }
}

@Composable
private fun BackupListScreen(state: RestoreCloudBackupUM.BackupList, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.items.forEach { row -> BackupRow(row) }
        }
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
                style = TangemTheme.typography.subtitle1,
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
                style = TangemTheme.typography.body2,
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
private fun TitleBlock(title: String, description: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = TangemTheme.typography.h3,
            color = TangemTheme.colors3.text.primary,
        )
        Text(
            text = description,
            style = TangemTheme.typography.body2,
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