package com.tangem.features.jointaccount.deeplink

import android.net.Uri
import kotlinx.coroutines.CoroutineScope

interface JointAccountInviteDeepLinkHandler {

    interface Factory {
        fun create(coroutineScope: CoroutineScope, deeplinkUri: Uri): JointAccountInviteDeepLinkHandler
    }
}