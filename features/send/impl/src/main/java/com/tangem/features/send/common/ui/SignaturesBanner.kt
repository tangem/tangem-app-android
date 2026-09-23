package com.tangem.features.send.common.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.common.ui.cosign.CosignRequestBanner
import com.tangem.common.ui.cosign.CosignRequestBannerUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_document_20
import com.tangem.features.send.common.ui.state.SendSignaturesUM
import com.tangem.features.send.impl.R

@Composable
internal fun SignaturesBanner(signaturesUM: SendSignaturesUM, modifier: Modifier = Modifier) {
    CosignRequestBanner(
        modifier = modifier,
        state = CosignRequestBannerUM(
            title = resourceReference(
                id = R.string.joint_account_send_signatures_required,
                formatArgs = wrappedList(signaturesUM.threshold.toString(), signaturesUM.membersCount.toString()),
            ),
            subtitle = resourceReference(
                if (signaturesUM.isFirstOperation) {
                    R.string.joint_account_send_sends_automatically_setup_fee
                } else {
                    R.string.joint_account_send_sends_automatically
                },
            ),
            trailingIcon = TangemIconUM.Icon(
                imageVector = Icons.ic_document_20,
                tintReference = { TangemTheme.colors3.icon.status.warning },
            ),
        ),
    )
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_SignaturesBanner() {
    TangemThemePreviewRedesign {
        SignaturesBanner(signaturesUM = SendSignaturesUM(threshold = 3, membersCount = 5))
    }
}