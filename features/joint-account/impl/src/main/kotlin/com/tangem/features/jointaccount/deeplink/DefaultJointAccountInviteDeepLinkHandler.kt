package com.tangem.features.jointaccount.deeplink

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.utils.pushIfAbsent
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val PATH_SEGMENT_JOINT_ACCOUNT = "joint-account"
private const val PATH_SEGMENT_JOIN = "join"
private const val EXPECTED_PATH_SEGMENTS_COUNT = 3

internal class DefaultJointAccountInviteDeepLinkHandler @AssistedInject constructor(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val deeplinkUri: Uri,
    private val appRouter: AppRouter,
    private val jointAccountFeatureToggles: JointAccountFeatureToggles,
) : JointAccountInviteDeepLinkHandler {

    init {
        handleDeepLink()
    }

    private fun handleDeepLink() {
        if (!jointAccountFeatureToggles.isJointAccountEnabled) {
            TangemLogger.e("Joint account feature is disabled, ignoring deeplink")
            appRouter.popTo(AppRoute.Wallet)
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
            appRouter.popTo(AppRoute.Wallet)
            return
        }

        scope.launch {
            appRouter.pushIfAbsent(AppRoute.JointAccountJoin(inviteId = inviteId))
        }
    }

    @AssistedFactory
    interface Factory : JointAccountInviteDeepLinkHandler.Factory {
        override fun create(coroutineScope: CoroutineScope, deeplinkUri: Uri): DefaultJointAccountInviteDeepLinkHandler
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