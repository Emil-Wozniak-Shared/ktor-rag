package model.document.xwiki.webpage


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Hierarchy(
    @SerialName("items")
    val items: List<Item>
)