package pl.service.ai

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import java.io.File

object ITextPDFExtractor {
    fun extractTextFromPDF(filePath: String): String {
        return try {
            val reader = PdfReader(File(filePath))
            val pdfDocument = PdfDocument(reader)
            val text = StringBuilder()

            (1..pdfDocument.numberOfPages).forEach { i ->
                text.append(PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i)))
            }

            pdfDocument.close()
            text.toString()
        } catch (e: Exception) {
            println("Error reading PDF: ${e.message}")
            ""
        }
    }

    fun extractTextFromPage(filePath: String, pageNumber: Int): String {
        return try {
            val reader = PdfReader(File(filePath))
            val pdfDocument = PdfDocument(reader)

            if (pageNumber > pdfDocument.numberOfPages) {
                println("Page number exceeds total pages")
                return ""
            }

            val text = PdfTextExtractor.getTextFromPage(pdfDocument.getPage(pageNumber))
            pdfDocument.close()
            text
        } catch (e: Exception) {
            println("Error reading PDF: ${e.message}")
            ""
        }
    }

    fun extractTextFromBytes(pdfBytes: ByteArray): String {
        return try {
            val reader = PdfReader(pdfBytes.inputStream())
            val pdfDocument = PdfDocument(reader)
            val text = StringBuilder()

            for (i in 1..pdfDocument.numberOfPages) {
                text.append(PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i)))
            }

            pdfDocument.close()
            text.toString()
        } catch (e: Exception) {
            println("Error reading PDF: ${e.message}")
            ""
        }
    }
}