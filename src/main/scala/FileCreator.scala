import java.io.ByteArrayOutputStream

import com.itextpdf.text.{Chunk, Document, Font, Paragraph}
import com.itextpdf.text.pdf.{BaseFont, PdfPCell, PdfPTable, PdfWriter}
import domain.InternetBankPayment

import scala.util.Try

object FileCreator {
  private val font = BaseFont.createFont(
    getClass.getResource("fonts/ptsans.ttf").toString,
    BaseFont.IDENTITY_H,
    BaseFont.EMBEDDED)

  private def c(s: String) = new PdfPCell(new Paragraph(new Chunk(s, new Font(font, 15))))

  def getPDF(payment: InternetBankPayment): Option[Array[Byte]] = Try {
    val document = new Document()
    val out = new ByteArrayOutputStream
    PdfWriter.getInstance(document, out)

    val table = new PdfPTable(2)

    table.addCell(c("ИНН"))
    table.addCell(c(payment.taxId))
    table.addCell(c("БИК"))
    table.addCell(c(payment.BIC))
    table.addCell(c("Номер счета"))
    table.addCell(c(payment.accountNumber))
    table.addCell(c("НДС"))
    table.addCell(c(payment.VAT.toString))
    table.addCell(c("Сумма"))
    table.addCell(c(payment.money.toString + " \u20BD"))

    document.open()
    document.add(table)
    document.close()

    out.toByteArray
  }.toOption
}
