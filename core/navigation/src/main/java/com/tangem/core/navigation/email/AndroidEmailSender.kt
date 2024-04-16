package com.tangem.core.navigation.email

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import timber.log.Timber

/**
 * Implementation of email sender for Android
 *
 * @property context context
 *
[REDACTED_AUTHOR]
 */
internal class AndroidEmailSender(private val context: Context) : EmailSender {

    override fun send(email: EmailSender.Email, onFail: ((Exception) -> Unit)?) {
        val originalIntent = createEmailShareIntent(email)
        val emailFilterIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))

        val packageManager = context.packageManager
        val originalIntentResults = packageManager.queryIntentActivities(originalIntent, 0)
        val emailFilterIntentResults = packageManager.queryIntentActivities(emailFilterIntent, 0)

        val targetedIntents = originalIntentResults
            .filter { originalResult ->
                emailFilterIntentResults.any {
                    originalResult.activityInfo.packageName == it.activityInfo.packageName
                }
            }
            .map {
                createEmailShareIntent(email).apply { setPackage(it.activityInfo.packageName) }
            }
            .toMutableList()

        try {
            val chooserIntent = Intent.createChooser(targetedIntents.removeAt(0), "Send mail...").apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedIntents.toTypedArray())
            }

            ContextCompat.startActivity(context, chooserIntent, null)
        } catch (ex: Exception) {
            Timber.e("Failed to send email: $ex")
        }
    }

    private fun createEmailShareIntent(email: EmailSender.Email): Intent {
        val builder = ShareCompat.IntentBuilder(context)
            .setType("message/rfc822")
            .setEmailTo(arrayOf(email.address))
            .setSubject(email.subject)
            .setText(email.message)

        email.attachment?.let {
            builder.setStream(
                FileProvider.getUriForFile(context, "${context.packageName}.provider", it),
            )
        }

        return builder.intent
    }
}