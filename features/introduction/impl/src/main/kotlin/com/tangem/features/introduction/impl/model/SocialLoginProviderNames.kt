package com.tangem.features.introduction.impl.model

import com.tangem.domain.sociallogin.models.SocialLoginProvider

internal fun SocialLoginProvider.displayName(): String = when (this) {
    SocialLoginProvider.Google -> "Google"
}

internal fun SocialLoginProvider.cloudName(): String = when (this) {
    SocialLoginProvider.Google -> "Google Drive"
}