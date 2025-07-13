package model.document.xwiki.webpage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class XWikiWebPage(
    @SerialName("attachments")
    val attachments: String?,
    @SerialName("author")
    val author: String,
    @SerialName("authorName")
    val authorName: String?,
    @SerialName("clazz")
    val clazz: String?,
    @SerialName("comment")
    val comment: String,
    @SerialName("content")
    val content: String,
    @SerialName("created")
    val created: Long,
    @SerialName("creator")
    val creator: String,
    @SerialName("creatorName")
    val creatorName: String?,
    @SerialName("enforceRequiredRights")
    val enforceRequiredRights: Boolean,
    @SerialName("fullName")
    val fullName: String,
    @SerialName("hidden")
    val hidden: Boolean,
    @SerialName("hierarchy")
    val hierarchy: Hierarchy,
    @SerialName("id")
    val id: String,
    @SerialName("language")
    val language: String,
    @SerialName("links")
    val links: List<Link>,
    @SerialName("majorVersion")
    val majorVersion: Int,
    @SerialName("minorVersion")
    val minorVersion: Int,
    @SerialName("modified")
    val modified: Long,
    @SerialName("modifier")
    val modifier: String,
    @SerialName("modifierName")
    val modifierName: String?,
    @SerialName("name")
    val name: String,
    @SerialName("objects")
    val objects: String?,
    @SerialName("originalMetadataAuthor")
    val originalMetadataAuthor: String,
    @SerialName("originalMetadataAuthorName")
    val originalMetadataAuthorName: String?,
    @SerialName("parent")
    val parent: String,
    @SerialName("parentId")
    val parentId: String,
    @SerialName("rawTitle")
    val rawTitle: String,
    @SerialName("space")
    val space: String,
    @SerialName("syntax")
    val syntax: String,
    @SerialName("title")
    val title: String,
    @SerialName("translations")
    val translations: Translations,
    @SerialName("version")
    val version: String,
    @SerialName("wiki")
    val wiki: String,
    @SerialName("xwikiAbsoluteUrl")
    val xwikiAbsoluteUrl: String,
    @SerialName("xwikiRelativeUrl")
    val xwikiRelativeUrl: String
)