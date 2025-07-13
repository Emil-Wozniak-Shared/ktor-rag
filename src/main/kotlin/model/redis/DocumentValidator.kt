package pl.model.redis

import pl.model.Validator

internal class SearchRequestValidator(
    private val subject: SearchRequest
) : Validator<SearchRequest>(subject) {
    override fun check() {
        if (subject.query.isEmpty() || subject.query.length < 5) {
            reasons += ValidationProblem("query","Query must have at least 5 characters")
        }
        if (subject.limit <= 0) {
            reasons += ValidationProblem("subject","Must be greater then 0")
        }
    }
}

internal class DocumentRequestValidator(
    private val subject: DocumentRequest
) : Validator<DocumentRequest>(subject) {
    override fun check() {
        if (subject.title.isEmpty() || subject.title.length < 5) {
            reasons += ValidationProblem("title","Must have at least 5 characters")
        }
        if (subject.content.isEmpty() || subject.content.length < 5) {
            reasons += ValidationProblem("content","Must have at least 5 characters")
        }
    }
}