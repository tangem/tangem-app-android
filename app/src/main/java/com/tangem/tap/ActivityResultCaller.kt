package com.tangem.tap

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher

interface ActivityResultCaller {
    val activityResultLauncher: ActivityResultLauncher<Intent>?
}
