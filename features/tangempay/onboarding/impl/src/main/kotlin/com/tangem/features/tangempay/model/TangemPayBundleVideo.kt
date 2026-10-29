package com.tangem.features.tangempay.model

import com.tangem.core.media.LoopingVideoController
import com.tangem.core.media.VideoFrameSize
import com.tangem.features.tangempay.onboarding.impl.R
import kotlinx.coroutines.CoroutineScope

/** Must match `R.raw.tangem_pay_bundle_background`: a file of another size is only logged. */
private val BUNDLE_VIDEO_SIZE = VideoFrameSize(width = 1080, height = 1920)

internal fun LoopingVideoController.Factory.createPayBundleVideo(scope: CoroutineScope): LoopingVideoController =
    create(
        scope = scope,
        videoRes = R.raw.tangem_pay_bundle_background,
        frameSize = BUNDLE_VIDEO_SIZE,
        diagnosticsSubject = "Pay bundle video",
    )