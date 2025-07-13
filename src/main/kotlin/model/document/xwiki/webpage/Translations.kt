package model.document.xwiki.webpage


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Translations(
    @SerialName("default")
    val default: String,
    @SerialName("links")
    val links: List<String?>,
    @SerialName("translations")
    val translations: List<String?>
)