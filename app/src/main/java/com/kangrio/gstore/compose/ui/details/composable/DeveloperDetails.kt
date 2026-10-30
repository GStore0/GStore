/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.compose.ui.details.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.kangrio.gstore.extensions.browse
import com.kangrio.gstore.extensions.copyToClipBoard
import com.kangrio.gstore.extensions.mailTo
import com.aurora.gplayapi.data.models.App
import com.kangrio.gstore.R
import com.kangrio.gstore.compose.composable.Info
import com.kangrio.gstore.compose.composable.SectionHeader
import com.kangrio.gstore.compose.preview.AppPreviewProvider
import com.kangrio.gstore.compose.preview.ThemePreviewProvider

/**
 * Composable to display details of the app developer, supposed to be used as a part
 * of the Column with proper vertical arrangement spacing in the AppDetailsScreen.
 * @param address Address of the app developer
 * @param website Website of the app developer
 * @param email Email address of the app developer
 */
@Composable
fun DeveloperDetails(address: String, website: String, email: String) {
    val context = LocalContext.current

    SectionHeader(title = stringResource(R.string.details_dev_details))
    Column {
        if (website.isNotBlank()) {
            Info(
                title = AnnotatedString(text = stringResource(R.string.details_dev_website)),
                description = AnnotatedString(text = website),
                painter = painterResource(R.drawable.ic_network),
                onClick = { context.browse(website) }
            )
        }

        if (email.isNotBlank()) {
            Info(
                title = AnnotatedString(text = stringResource(R.string.details_dev_email)),
                description = AnnotatedString(text = email),
                painter = painterResource(R.drawable.ic_mail),
                onClick = { context.mailTo(email) }
            )
        }

        if (address.isNotBlank()) {
            Info(
                title = AnnotatedString(text = stringResource(R.string.details_dev_address)),
                description = AnnotatedString.fromHtml(htmlString = address),
                painter = painterResource(R.drawable.ic_person_location),
                onClick = { context.copyToClipBoard(address) }
            )
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun DeveloperDetailsPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        DeveloperDetails(
            address = app.developerAddress,
            website = app.developerWebsite,
            email = app.developerEmail
        )
    }
}
