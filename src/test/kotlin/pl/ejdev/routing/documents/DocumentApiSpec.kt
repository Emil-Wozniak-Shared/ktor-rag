package pl.pl.ejdev.routing.documents

import pl.pl.ejdev.routing.ApiSpec

open class DocumentApiSpec(): ApiSpec() {

    protected companion object {
        const val PATH = "/api/documents"
        const val TITLE = "Tytuł testowy"
        const val TEST_CONTENT = "test content"
        const val SHORT_TITLE = "test"
    }
}