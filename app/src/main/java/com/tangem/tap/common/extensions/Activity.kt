package com.tangem.tap.common.extensions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/**
 * required since targetAndroid=30
 * <queries>
 *     <intent>
 *         <action android:name="android.intent.action.SENDTO" />
 *         <data android:scheme="mailto" />
 *     </intent>
 * </queries>
 */
fun Activity.sendEmail(
    email: String,
    subject: String,
    message: String,
    file: File? = null,
    onFail: ((Exception) -> Unit)? = null
) {
    fun createEmailShareIntent(recipient: String, subject: String, text: String, file: File? = null): Intent {
        val builder = ShareCompat.IntentBuilder.from(this)
            .setType("message/rfc822")
            .setEmailTo(arrayOf(recipient))
            .setSubject(subject)
            .setText(text)
        file?.let { builder.setStream(FileProvider.getUriForFile(this, "$packageName.provider", it)) }
        return builder.intent
    }

    val originalIntent = createEmailShareIntent(email, subject, message, file)
    val emailFilterIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))

    val originalIntentResults = packageManager.queryIntentActivities(originalIntent, 0)
    val emailFilterIntentResults = packageManager.queryIntentActivities(emailFilterIntent, 0)

    val targetedIntents = originalIntentResults
        .filter { originalResult ->
            emailFilterIntentResults.any {
                originalResult.activityInfo.packageName == it.activityInfo.packageName
            }
        }
        .map {
            createEmailShareIntent(email, subject, message, file).apply {
                setPackage(it.activityInfo.packageName)
            }
        }
        .toMutableList()
    try {
        val chooserIntent = Intent.createChooser(targetedIntents.removeAt(0), "Send mail...")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedIntents.toTypedArray())
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ContextCompat.startActivity(this, chooserIntent, null)
    } catch (ex: Exception) {
        onFail?.invoke(ex)
    }
}