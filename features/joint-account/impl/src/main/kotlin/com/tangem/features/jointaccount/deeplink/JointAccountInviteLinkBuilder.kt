package com.tangem.features.jointaccount.deeplink

import java.net.URLEncoder

internal object JointAccountInviteLinkBuilder {

    private const val BASE_URL = "https://tangem.com/joint-account/join"

    /** The id is a path segment: reserved characters from the backend must not change the link's meaning */
    fun build(inviteId: String): String {
        val encoded = URLEncoder.encode(inviteId, Charsets.UTF_8.name()).replace(oldValue = "+", newValue = "%20")
        return "$BASE_URL/$encoded"
    }
}