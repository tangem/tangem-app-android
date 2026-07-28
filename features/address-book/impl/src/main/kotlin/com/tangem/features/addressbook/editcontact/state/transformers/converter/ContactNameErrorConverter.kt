package com.tangem.features.addressbook.editcontact.state.transformers.converter

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.addressbook.error.ContactNameValidationError
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.utils.converter.Converter

internal class ContactNameErrorConverter : Converter<ContactNameValidationError, TextReference> {

    override fun convert(value: ContactNameValidationError): TextReference = when (value) {
        ContactNameValidationError.Duplicate -> resourceReference(R.string.address_book_name_taken_error)
        is ContactNameValidationError.Format -> when (value.error) {
            ContactName.Error.ExceedsMaxLength -> resourceReference(R.string.address_book_name_max_chars_error)
            ContactName.Error.InvalidCharacters -> resourceReference(R.string.address_book_name_invalid_chars_error)
            ContactName.Error.Empty -> resourceReference(R.string.address_book_name_empty_error)
        }
    }
}