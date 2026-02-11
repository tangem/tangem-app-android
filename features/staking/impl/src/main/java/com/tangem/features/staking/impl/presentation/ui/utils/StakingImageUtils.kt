package com.tangem.features.staking.impl.presentation.ui.utils

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.R as CoreR
import com.tangem.domain.staking.model.StakingLocalImageType
import com.tangem.domain.staking.model.StakingTargetImage

fun StakingTargetImage?.toImageReference(): ImageReference? = when (this) {
    is StakingTargetImage.Url -> url.takeIf { it.isNotBlank() }?.let { ImageReference.Url(it) }
    is StakingTargetImage.Local -> ImageReference.Res(type.toDrawableRes())
    null -> null
}

fun StakingTargetImage?.toImageUrl(): String? = (this as? StakingTargetImage.Url)?.url?.takeIf { it.isNotBlank() }

@DrawableRes
fun StakingTargetImage?.toDrawableRes(): Int? = (this as? StakingTargetImage.Local)?.type?.toDrawableRes()

@DrawableRes
fun StakingLocalImageType.toDrawableRes(): Int = when (this) {
    StakingLocalImageType.P2P_VAULT -> CoreR.drawable.ic_p2p_logo
}