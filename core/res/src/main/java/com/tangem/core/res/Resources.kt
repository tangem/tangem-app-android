package com.tangem.core.res

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.utils.SupportedLanguages
import timber.log.Timber

/**
 * Get a string resource safely or the resource name if an exception is thrown
 *
 * @param id resource id
 */
fun Resources.getStringSafe(@StringRes id: Int): String {
    return runCatching { getString(id) }
        .getOrResourceName(resources = this, id)
}

/**
 * Get a string resource safely or the resource name if an exception is thrown
 *
 * @param id         resource id
 * @param formatArgs format args
 */
fun Resources.getStringSafe(@StringRes id: Int, vararg formatArgs: Any): String {
    return runCatching { getString(id, *formatArgs) }
        .recoverCatching {
            // If something goes wrong, returns the resource without arguments
            val string = getString(id)

            reportIssue(it, resources = this, id, *formatArgs)

            string
        }
        .getOrResourceName(resources = this, id, *formatArgs)
}

/**
 * Get a plural string resource safely or the resource name if an exception is thrown
 *
 * @param id         resource id
 * @param count      count
 */
fun Resources.getPluralStringSafe(@PluralsRes id: Int, count: Int): String {
    return runCatching { getQuantityString(id, count) }
        .getOrResourceName(resources = this, id)
}

/**
 * Get a plural string resource safely or the resource name if an exception is thrown
 *
 * @param id         resource id
 * @param count      count
 * @param formatArgs format args
 */
fun Resources.getPluralStringSafe(@PluralsRes id: Int, count: Int, vararg formatArgs: Any): String {
    return runCatching { getQuantityString(id, count, *formatArgs) }
        .getOrResourceName(resources = this, id, *formatArgs)
}

private fun Result<String>.getOrResourceName(resources: Resources, id: Int, vararg formatArgs: Any): String {
    return getOrElse {
        reportIssue(it, resources, id, formatArgs)

        // If something still goes wrong, returns the resource name
        resources.getResourceEntryName(id)
    }
}

private fun reportIssue(throwable: Throwable, resources: Resources, id: Int, vararg formatArgs: Any) {
    val exception = IllegalStateException(
        "An error occurred while parsing the string:\n" +
            "\tname: R.string.${resources.getResourceEntryName(id)}\n" +
            "\targs: ${formatArgs.joinToString(prefix = "[", postfix = "]") { it.toString() }}\n" +
            "\tlocale: ${SupportedLanguages.getCurrentSupportedLanguageCode()}\n" +
            "\terror message: ${throwable.message.orEmpty()}\n",
    )

    Timber.tag("Resources").e(exception)

    FirebaseCrashlytics.getInstance().recordException(exception)
}