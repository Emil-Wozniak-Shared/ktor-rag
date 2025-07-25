package model.document.xwiki.space

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Space(
    @SerialName("home")
    val home: String? = null,
    @SerialName("id")
    val id: String? = null,
    @SerialName("links")
    val links: List<Link>,
    @SerialName("name")
    val name: String? = null,
    @SerialName("wiki")
    val wiki: String? = null,
    @SerialName("xwikiAbsoluteUrl")
    val xwikiAbsoluteUrl: String? = null,
    @SerialName("xwikiRelativeUrl")
    val xwikiRelativeUrl: String? = null
)