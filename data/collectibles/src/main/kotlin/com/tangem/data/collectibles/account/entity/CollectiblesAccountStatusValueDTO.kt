package com.tangem.data.collectibles.account.entity

/**
 * On-disk representation of the collectibles account status, kept separate from the
 * [com.tangem.domain.models.account.CollectiblesAccountStatusValue] domain model.
 *
 * The constant names are the file format — they are what gets written — so renaming one invalidates every record
 * already on a device. `CollectiblesAccountStatusValueDTOTest` pins them for that reason.
 *
 * Two things are deliberately absent. There is no status source: a restored value has not been re-read from the
 * chain in this session by definition, so the converter always brings it back as cached. And there is no
 * representation of the loading, not-onboarded and error states, so a momentary failure cannot be persisted and
 * then read back as if it were the account's real state.
 */
internal enum class CollectiblesAccountStatusValueDTO {

    /** The vault exists and answers to the delegate this device holds. */
    ONBOARDED,

    /** The vault exists but answers to another key, so it has to be re-pointed before it can sign. */
    UPDATE_REQUIRED,
}