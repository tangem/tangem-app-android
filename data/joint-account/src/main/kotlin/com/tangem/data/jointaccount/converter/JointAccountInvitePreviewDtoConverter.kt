package com.tangem.data.jointaccount.converter

import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountInvitePreview
import com.tangem.store.datasource.jointaccount.models.JointAccountInvitePreviewDto
import com.tangem.utils.converter.Converter
import javax.inject.Inject
import javax.inject.Singleton

/** Network DTO → domain. */
@Singleton
internal class JointAccountInvitePreviewDtoConverter @Inject constructor() :
    Converter<JointAccountInvitePreviewDto, JointAccountInvitePreview> {

    override fun convert(value: JointAccountInvitePreviewDto): JointAccountInvitePreview = JointAccountInvitePreview(
        config = JointAccountConfig(
            name = value.config.name,
            icon = value.config.icon,
            iconColor = value.config.iconColor,
            membersCount = value.config.membersCount,
            threshold = value.config.threshold,
        ),
        creator = JointAccountInvitePreview.Creator(
            name = value.creator.name,
            address = value.creator.address,
        ),
    )
}