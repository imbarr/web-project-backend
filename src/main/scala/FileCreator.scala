import domain.InternetBankPayment
import org.apache.pdfbox.pdmodel.{PDDocument, PDPage}
import java.io.ByteArrayOutputStream

object FileCreator {
  def getPDF(payment: InternetBankPayment): Array[Byte] = {
    val document = new PDDocument
    document.addPage(new PDPage)
    val out = new ByteArrayOutputStream
    document.save(out)
    document.close()
    out.toByteArray
  }
}
