package com.tangem.features.txhistory.joint.operation.signers.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountIcon
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_checkmark_16
import com.tangem.core.ui.res.generated.icons.ic_cross_16
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.txhistory.joint.operation.signers.ui.state.JointOperationSignersUM
import kotlinx.collections.immutable.persistentListOf
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun JointOperationSignersContent(state: JointOperationSignersUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        state.signers.forEach { signer ->
            SignerRow(signer = signer)
        }
    }
}

@Composable
private fun SignerRow(signer: JointOperationSignersUM.SignerUM, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier,
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
        titleSlot = {
            Text(
                text = signer.name,
                color = when (signer.mark) {
                    null -> TangemTheme.colors3.text.secondary
                    else -> TangemTheme.colors3.text.primary
                },
                style = TangemTheme.typography3.body.medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        subtitleSlot = signer.statusText.takeIf { it != TextReference.EMPTY }?.let { status ->
            { TangemRowText(text = status, role = TangemRowTextRole.Subtitle) }
        },
        endSlot = {
            signer.mark?.let { mark ->
                AnswerMark(mark = mark, namesTheAnswer = signer.statusText == TextReference.EMPTY)
            }
        },
    )
}

/**
 * @param namesTheAnswer the mark is the only sign of the answer, so a screen reader has to read it out; where the
 * status subtitle spells the answer out, describing the mark too would say it twice
 */
@Composable
private fun AnswerMark(mark: JointOperationSignersUM.Mark, namesTheAnswer: Boolean, modifier: Modifier = Modifier) {
    val (icon, background, tint) = when (mark) {
        JointOperationSignersUM.Mark.Signed -> Triple(
            Icons.ic_checkmark_16,
            TangemTheme.colors3.bg.status.successSubtle,
            TangemTheme.colors3.icon.status.success,
        )
        JointOperationSignersUM.Mark.Declined -> Triple(
            Icons.ic_cross_16,
            TangemTheme.colors3.bg.status.errorSubtle,
            TangemTheme.colors3.icon.status.error,
        )
    }

    val description = if (namesTheAnswer) {
        stringResourceSafe(
            id = when (mark) {
                JointOperationSignersUM.Mark.Signed -> CoreUiR.string.joint_operation_signed
                JointOperationSignersUM.Mark.Declined -> CoreUiR.string.joint_operation_declined
            },
        )
    } else {
        null
    }

    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(background)
            .then(description?.let { Modifier.semantics { contentDescription = it } } ?: Modifier),
        contentAlignment = Alignment.Center,
    ) {
        TangemIcon(tangemIconUM = TangemIconUM.Icon(imageVector = icon, tintReference = { tint }))
    }
}

// region Preview
@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun JointOperationSignersContentPreview() {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.background(TangemTheme.colors3.bg.secondary)) {
            JointOperationSignersContent(
                state = JointOperationSignersUM(
                    membersCount = 5,
                    signers = persistentListOf(
                        signerPreview(name = "Isaac", status = CoreUiR.string.joint_operation_created_at),
                        signerPreview(name = "Kingston", status = CoreUiR.string.joint_operation_signed_at),
                        signerPreview(name = "Daniel", status = CoreUiR.string.joint_operation_declined_at),
                        signerPreview(name = "Olivia", status = CoreUiR.string.joint_operation_waiting_to_sign),
                        signerPreview(name = "Yasmin", status = CoreUiR.string.joint_operation_waiting_to_sign),
                    ),
                ),
            )
        }
    }
}

private fun signerPreview(name: String, status: Int): JointOperationSignersUM.SignerUM {
    val mark = when (status) {
        CoreUiR.string.joint_operation_created_at, CoreUiR.string.joint_operation_signed_at ->
            JointOperationSignersUM.Mark.Signed
        CoreUiR.string.joint_operation_declined_at -> JointOperationSignersUM.Mark.Declined
        else -> null
    }
    val colors = CryptoPortfolioIcon.Color.entries

    return JointOperationSignersUM.SignerUM(
        name = name,
        avatar = JointAccountMembersUM.MemberAvatarUM(
            monogram = name.take(n = 1),
            color = colors[name.length % colors.size],
        ),
        statusText = if (mark != null) {
            resourceReference(id = status, formatArgs = wrappedList("15:34, Today"))
        } else {
            resourceReference(status)
        },
        mark = mark,
    )
}
// endregion