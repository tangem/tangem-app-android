package com.tangem.features.earn.impl.picker

import com.tangem.core.media.VideoFrameSize

internal object EarnPickerHeroVideo {

    const val URL: String =
        "https://s3.eu-central-1.amazonaws.com/tangem.api/stories/assets/" +
            "hf-20260819-122837-8c76fe44-bd87-4241-b7bf-150f9f50fb45-compressed-1794559392823-5f362bca23ae.mp4"

    // Must match the file: a file of another size is only logged
    val FRAME_SIZE: VideoFrameSize = VideoFrameSize(width = 720, height = 1280)

    const val DIAGNOSTICS_SUBJECT: String = "Earn picker hero video"
}