package model.document.xwiki.space


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Space(
    @SerialName("home")
    val home: String,
    @SerialName("id")
    val id: String,
    @SerialName("links")
    val links: List<Link>,
    @SerialName("name")
    val name: String,
    @SerialName("wiki")
    val wiki: String,
    @SerialName("xwikiAbsoluteUrl")
    val xwikiAbsoluteUrl: String,
    @SerialName("xwikiRelativeUrl")
    val xwikiRelativeUrl: String
)