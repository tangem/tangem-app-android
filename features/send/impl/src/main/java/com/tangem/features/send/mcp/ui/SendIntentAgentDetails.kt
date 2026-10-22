package com.tangem.features.send.mcp.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.send.mcp.model.SendIntentAgentUM

/**
 * Who asks for the send and what they ask: the agent's icon and verified host, or a generic name, over the request.
 *
 * [Figma](https://www.figma.com/design/Qqm0dNTOnqtxLYEcmgc32C/Store?node-id=25973-248232)
 */
@Composable
internal fun SendIntentAgentDetails(
    agent: SendIntentAgentUM,
    description: TextReference,
    modifier: Modifier = Modifier,
) {
    TangemSurface(
        modifier = modifier.fillMaxWidth(),
        color = TangemTheme.colors3.bg.tertiary,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AgentIdentity(iconUrl = agent.iconUrl, label = agent.label)
            Text(
                text = description.resolveReference(),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
            )
        }
    }
}

@Composable
private fun AgentIdentity(iconUrl: String?, label: TextReference) {
    val unknownAgent = painterResource(R.drawable.ic_mcp_unknown_agent_16)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp)),
            model = iconUrl,
            placeholder = unknownAgent,
            error = unknownAgent,
            fallback = unknownAgent,
            contentDescription = null,
        )
        Text(
            text = label.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SendIntentAgentDetailsPreview() {
    TangemThemePreviewRedesign {
        SendIntentAgentDetails(
            agent = SendIntentAgentUM.Verified(iconUrl = null, label = stringReference("mcp.claude.com")),
            description = stringReference(
                "The agent is asking to send from your account. You’ll review the details next.",
            ),
            modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SendIntentUnknownAgentDetailsPreview() {
    TangemThemePreviewRedesign {
        SendIntentAgentDetails(
            agent = SendIntentAgentUM.Unknown(iconUrl = null, label = stringReference("Unknown")),
            description = stringReference(
                "The agent is asking to send from your account. You’ll review the details next.",
            ),
            modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
        )
    }
}