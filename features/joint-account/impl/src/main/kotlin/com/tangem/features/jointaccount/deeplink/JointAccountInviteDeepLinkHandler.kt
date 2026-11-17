package com.tangem.features.jointaccount.deeplink

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.navigation.deeplink.DeepLinkHandler
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PATH_SEGMENT_JOINT_ACCOUNT = "joint-account"
private const val PATH_SEGMENT_JOIN = "join"
private const val EXPECTED_PATH_SEGMENTS_COUNT = 3

internal class JointAccountInviteDeepLinkHandler @Inject constructor(
    private val jointAccountFeatureToggles: JointAccountFeatureToggles,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : DeepLinkHandler {

    override val isEnabled: Boolean
        get() = jointAccountFeatureToggles.isJointAccountEnabled

    override fun handle(request: DeepLinkRequest) {
        val router = request.router
        val deeplinkUri = request.uri
        if (!jointAccountFeatureToggles.isJointAccountEnabled) {
            TangemLogger.e("Joint account feature is disabled, ignoring deeplink")
            router.popTo(AppRoute.Wallet)
            return
        }

        val inviteId = extractInviteIdFromUri(deeplinkUri)
        if (inviteId == null) {
            TangemLogger.e(
                """
                    Failed to extract invite id from deep link
                    |- Received URI: $deeplinkUri
                """.trimIndent(),
            )
            router.popTo(AppRoute.Wallet)
            return
        }

        request.scope.launch {
            val route = AppRoute.JointAccountJoin(inviteId = inviteId)
            if (router.contains(route)) return@launch

            analyticsEventHandler.send(JointAccountAnalyticsEvent.InviteLinkOpened())
            router.push(route)
        }
    }

    companion object {
        const val DEEPLINK_KEY =
            "https://tangem.com/$PATH_SEGMENT_JOINT_ACCOUNT/$PATH_SEGMENT_JOIN"
    }
}

/** Parses `https://tangem.com/joint-account/join/{inviteId}` into the invite id. */
private fun extractInviteIdFromUri(uri: Uri): String? {
    val segments = uri.pathSegments
    val isValidPath = segments.size == EXPECTED_PATH_SEGMENTS_COUNT &&
        segments[0] == PATH_SEGMENT_JOINT_ACCOUNT &&
        segments[1] == PATH_SEGMENT_JOIN
    return segments.lastOrNull().takeIf { isValidPath }
}