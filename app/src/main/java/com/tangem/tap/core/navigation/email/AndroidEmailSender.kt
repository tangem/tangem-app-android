package com.tangem.tap.core.navigation.email

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.tangem.core.navigation.email.EmailSender
import com.tangem.tap.foregroundActivityObserver
import timber.log.Timber

/**
 * Implementation of email sender for Android
 *
[REDACTED_AUTHOR]
 */
internal class AndroidEmailSender : EmailSender {

    override fun send(email: EmailSender.Email, onFail: ((Exception) -> Unit)?) {
        val activity = foregroundActivityObserver.foregroundActivity

        if (activity == null) {
            Timber.e("Foreground activity not found")
            return
        }

        val originalIntent = createEmailShareIntent(activity, email)
        val emailFilterIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))

        val packageManager = activity.packageManager
        val originalIntentResults = packageManager.queryIntentActivities(originalIntent, 0)
        val emailFilterIntentResults = packageManager.queryIntentActivities(emailFilterIntent, 0)

        val targetedIntents = originalIntentResults
            .filter { originalResult ->
                emailFilterIntentResults.any {
                    originalResult.activityInfo.packageName == it.activityInfo.packageName
                }
            }
            .map {
                createEmailShareIntent(activity, email).apply { setPackage(it.activityInfo.packageName) }
            }
            .toMutableList()

        try {
            val chooserIntent = Intent.createChooser(targetedIntents.removeAt(0), "Send mail...").apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedIntents.toTypedArray())
            }

            ContextCompat.startActivity(activity, chooserIntent, null)
        } catch (ex: Exception) {
            Timber.e("Failed to send email: $ex")
        }
    }

    private fun createEmailShareIntent(activity: AppCompatActivity, email: EmailSender.Email): Intent {
        val builder = ShareCompat.IntentBuilder(activity)
            .setType("message/rfc822")
            .setEmailTo(arrayOf(email.address))
            .setSubject(email.subject)
            .setText(email.message)

        email.attachment?.let { file ->
            builder.setStream(
                FileProvider.getUriForFile(activity, "${activity.packageName}.provider", file),
            )
        }

        return builder.intent
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}