package com.tangem.features.txhistory.joint.operation.signers.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountIcon
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.bottomsheets.LocalBottomSheetContentScrollable
import com.tangem.core.ui.components.bottomsheets.LocalTangemBottomSheetContentBottomInset
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_checkmark_20
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.txhistory.joint.operation.signers.ui.state.JointOperationSignersUM
import kotlinx.collections.immutable.persistentListOf
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun JointOperationSignersContent(state: JointOperationSignersUM, modifier: Modifier = Modifier) {
    val bottomInset = LocalTangemBottomSheetContentBottomInset.current
    val contentScrollable = LocalBottomSheetContentScrollable.current
    val scrollState = rememberScrollState()

    if (contentScrollable != null) {
        LaunchedEffect(scrollState) {
            snapshotFlow { scrollState.canScrollForward || scrollState.canScrollBackward }
                .distinctUntilChanged()
                .collect { isScrollable -> contentScrollable.value = isScrollable }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(bottom = bottomInset.coerceAtLeast(16.dp)),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (state) {
            is JointOperationSignersUM.Loading -> CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 24.dp)
                    .size(24.dp),
                color = TangemTheme.colors3.icon.secondary,
            )
            is JointOperationSignersUM.Content -> state.signers.forEach { signer ->
                SignerRow(signer = signer)
            }
        }
    }
}

@Composable
private fun SignerRow(signer: JointOperationSignersUM.SignerUM, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.secondary),
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = {
            AccountIcon(
                name = stringReference(signer.avatar.monogram),
                icon = AccountIconUM.CryptoPortfolio(
                    value = CryptoPortfolioIcon.Icon.Letter,
                    color = signer.avatar.color,
                ),
                size = AccountIconSize.ContactDefault,
            )
        },
        titleSlot = { TangemRowText(text = stringReference(signer.name), role = TangemRowTextRole.Title) },
        subtitleSlot = { TangemRowText(text = signer.statusText, role = TangemRowTextRole.Subtitle) },
        endSlot = {
            if (signer.isAnswered) {
                TangemIcon(
                    tangemIconUM = TangemIconUM.Icon(
                        imageVector = Icons.ic_checkmark_20,
                        tintReference = { TangemTheme.colors3.icon.status.success },
                    ),
                )
            }
        },
    )
}

// region Preview
@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun JointOperationSignersContentPreview() {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            JointOperationSignersContent(
                state = JointOperationSignersUM.Content(
                    membersCount = 5,
                    signers = persistentListOf(
                        signerPreview(name = "Isaac", status = CoreUiR.string.joint_operation_created_at),
                        signerPreview(name = "Kingston", status = CoreUiR.string.joint_operation_signed_at),
                        signerPreview(name = "Daniel", status = CoreUiR.string.joint_operation_waiting_to_sign),
                        signerPreview(name = "Olivia", status = CoreUiR.string.joint_operation_waiting_to_sign),
                        signerPreview(name = "Yasmin", status = CoreUiR.string.joint_operation_declined),
                    ),
                ),
            )
        }
    }
}

private fun signerPreview(name: String, status: Int): JointOperationSignersUM.SignerUM {
    val isAnswered = status == CoreUiR.string.joint_operation_created_at ||
        status == CoreUiR.string.joint_operation_signed_at
    val colors = CryptoPortfolioIcon.Color.entries

    return JointOperationSignersUM.SignerUM(
        name = name,
        avatar = JointAccountMembersUM.MemberAvatarUM(
            monogram = name.take(n = 1),
            color = colors[name.length % colors.size],
        ),
        statusText = if (isAnswered) {
            resourceReference(id = status, formatArgs = wrappedList("15:34, Today"))
        } else {
            resourceReference(status)
        },
        isAnswered = isAnswered,
    )
}
// endregion