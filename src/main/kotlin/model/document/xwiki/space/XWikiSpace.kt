package model.document.xwiki.space

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class XWikiSpace(
    @SerialName("links")
    val links: List<XWikiLink>,
    @SerialName("spaces")
    val spaces: List<Space>
)

@Serializable
class XWikiLink()