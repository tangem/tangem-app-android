package com.tangem.tap.common.chat.opener

import android.content.Context
import java.io.File

internal interface ChatOpener {
    fun open(createFeedbackFile: (Context) -> File?, createLogsFile: (Context) -> File?)
}
