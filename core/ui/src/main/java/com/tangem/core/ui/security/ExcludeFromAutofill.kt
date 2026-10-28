package com.tangem.core.ui.security

import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentDataType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics

/**
 * Keeps a text field out of autofill: the password manager neither offers credentials in it nor gets its value
 * to save — for a pin, an access code or a seed phrase word.
 *
 * Compose reports text fields to autofill through semantics, and a text field declares itself [ContentDataType.Text]
 * and, for a `Password`/`NumberPassword` keyboard, [ContentType.Password]. Outer semantics win over inner ones on
 * the same node, so this has to be in the modifier the text field itself receives, not on a parent.
 */
fun Modifier.excludeFromAutofill(): Modifier = semantics {
    contentDataType = ContentDataType.None
    contentType = NotACredential
}

private val NotACredential = ContentType("off")