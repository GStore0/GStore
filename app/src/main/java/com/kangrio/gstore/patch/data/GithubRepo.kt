package com.kangrio.gstore.patch.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubRepo(
    @SerialName("html_url") val html_url: String = String()
)
