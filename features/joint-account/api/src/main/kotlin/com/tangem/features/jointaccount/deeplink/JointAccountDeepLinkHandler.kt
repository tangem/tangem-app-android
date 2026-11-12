package com.tangem.features.jointaccount.deeplink

import android.net.Uri

interface JointAccountDeepLinkHandler {

    companion object {
        private const val SCHEME = "tangem"
        private const val HOST = "joint_account"

        const val DEEPLINK_KEY = "$SCHEME://$HOST"

        const val ENTRY_MEMBERS: String = "members"
        const val ENTRY_OVERVIEW: String = "overview"

        /** Whether [uri] is a joint account deep link: `tangem://joint_account`. */
        fun canHandle(uri: Uri): Boolean = uri.scheme == SCHEME && uri.host == HOST
    }
}