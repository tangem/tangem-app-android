package com.tangem.core.ui.components.transactions

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * Common transaction done screen title
 *
 * @param title title resource
 * @param subtitle subtitle text
 */
@Composable
fun TransactionDoneTitle(title: TextReference, subtitle: TextReference, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_empty_in_process_64),
            contentDescription = null,
            modifier = Modifier
                .size(TangemTheme.dimens.size64),
        )
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography.h3,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing16),
        )
        Text(
            text = subtitle.resolveReference(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing4),
        )
    }
}

// region Previews
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TransactionDoneTitlePreview() {
    TangemThemePreview {
        TransactionDoneTitle(
            title = resourceReference(R.string.sent_transaction_sent_title),
            subtitle = stringReference("0"),
            modifier = Modifier
                .background(TangemTheme.colors.background.tertiary)
                .padding(TangemTheme.dimens.spacing16),
        )
    }
}
// endregion