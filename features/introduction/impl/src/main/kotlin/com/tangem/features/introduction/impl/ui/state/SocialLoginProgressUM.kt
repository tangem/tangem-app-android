package com.tangem.features.introduction.impl.ui.state

import com.tangem.core.ui.extensions.TextReference

internal sealed interface SocialLoginProgressUM {

    data object Loader : SocialLoginProgressUM

    data class Stage(
        val title: TextReference,
        val description: TextReference? = null,
    ) : SocialLoginProgressUM
}