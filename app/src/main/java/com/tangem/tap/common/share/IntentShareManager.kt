package com.tangem.tap.common.share

import android.content.Intent
import com.tangem.core.navigation.share.ShareManager
import com.tangem.tap.foregroundActivityObserver
import com.tangem.tap.withForegroundActivity

internal class IntentShareManager : ShareManager {

    override fun shareText(text: String) {
        foregroundActivityObserver.withForegroundActivity { activity ->
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)

            activity.startActivity(shareIntent)
        }
    }
}
