package model.document.xwiki.webpage


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Link(
    @SerialName("href")
    val href: String,
    @SerialName("hrefLang")
    val hrefLang: String?,
    @SerialName("rel")
    val rel: String,
    @SerialName("type")
    val type: String?
)