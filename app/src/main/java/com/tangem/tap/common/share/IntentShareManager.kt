package com.tangem.tap.common.share

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tangem.core.navigation.share.ShareManager
import com.tangem.tap.foregroundActivityObserver
import com.tangem.tap.withForegroundActivity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class IntentShareManager @Inject constructor() : ShareManager {

    override val sharedTexts: SharedFlow<String>
        field = MutableSharedFlow<String>(extraBufferCapacity = 1)

    override fun shareText(text: String) {
        foregroundActivityObserver.withForegroundActivity { activity ->
            activity.startActivity(Intent.createChooser(sendIntent(text), null))
        }
    }

    override fun shareTrackedText(text: String) {
        foregroundActivityObserver.withForegroundActivity { activity ->
            val chooser = Intent.createChooser(
                sendIntent(text),
                null,
                shareResultSender(context = activity, text = text).intentSender,
            )
            activity.startActivity(chooser)
        }
    }

    /** Called by [ShareResultReceiver] once the user picks where to send [text] */
    fun onShareTargetChosen(text: String) {
        sharedTexts.tryEmit(text)
    }

    private fun sendIntent(text: String): Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }

    /**
     * One share sheet is open at a time, so one request code serves every share and [PendingIntent.FLAG_UPDATE_CURRENT]
     * puts the latest text in. The chooser fills in the picked target, so the intent stays mutable from Android 12 on.
     */
    private fun shareResultSender(context: Context, text: String): PendingIntent {
        val mutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

        return PendingIntent.getBroadcast(
            context,
            SHARE_RESULT_REQUEST_CODE,
            ShareResultReceiver.intent(context = context, text = text),
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag,
        )
    }

    private companion object {
        const val SHARE_RESULT_REQUEST_CODE = 0
    }
}