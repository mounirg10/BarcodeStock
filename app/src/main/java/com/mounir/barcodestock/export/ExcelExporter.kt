package com.mounir.barcodestock.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.mounir.barcodestock.data.Product
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * ينشئ ملف Excel حقيقي (.xlsx) بدون أي مكتبة خارجية،
 * عبر كتابة حزمة OOXML مباشرة داخل ملف ZIP.
 */
object ExcelExporter {

    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    private val stampFmt = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)

    private val headers = listOf(
        "الباركود", "اسم المنتج", "السعر", "الكمية", "الإجمالي",
        "تاريخ الدخول", "تاريخ نهاية الصلاحية", "الأيام المتبقية", "الحالة"
    )

    /** يكتب الملف في مجلد التطبيق ويرجع الملف الناتج. */
    fun export(context: Context, products: List<Product>): File {
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        val file = File(dir, "BarcodeStock_${stampFmt.format(Date())}.xlsx")

        val rows = StringBuilder()
        rows.append(row(1, headers))
        products.forEachIndexed { i, p ->
            rows.append(
                row(
                    i + 2,
                    listOf(
                        p.barcode,
                        p.name,
                        trim(p.price),
                        p.quantity.toString(),
                        trim(p.price * p.quantity),
                        dateFmt.format(Date(p.entryDate)),
                        dateFmt.format(Date(p.expiryDate)),
                        p.daysLeft.toString(),
                        p.status.label
                    )
                )
            )
        }

        val sheet = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>$rows</sheetData></worksheet>"""

        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            zip.put("[Content_Types].xml", CONTENT_TYPES)
            zip.put("_rels/.rels", RELS)
            zip.put("xl/workbook.xml", WORKBOOK)
            zip.put("xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            zip.put("xl/worksheets/sheet1.xml", sheet)
        }
        return file
    }

    /** يفتح قائمة المشاركة لحفظ النسخة (واتساب، إيميل، Drive، الملفات...). */
    fun share(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "حفظ / مشاركة النسخة الاحتياطية"))
    }

    // ---------- أدوات داخلية ----------

    private fun trim(v: Double) =
        if (v % 1.0 == 0.0) v.toLong().toString() else String.format(Locale.US, "%.2f", v)

    private fun row(index: Int, values: List<String>): String {
        val cells = values.mapIndexed { c, v ->
            """<c r="${col(c)}$index" t="inlineStr"><is><t xml:space="preserve">${esc(v)}</t></is></c>"""
        }.joinToString("")
        return """<row r="$index">$cells</row>"""
    }

    private fun col(i: Int): String {
        var n = i; val sb = StringBuilder()
        do { sb.insert(0, ('A' + n % 26)); n = n / 26 - 1 } while (n >= 0)
        return sb.toString()
    }

    private fun esc(s: String) = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun ZipOutputStream.put(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private const val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>"""

    private const val RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""

    private const val WORKBOOK = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Products" sheetId="1" r:id="rId1"/></sheets></workbook>"""

    private const val WORKBOOK_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>"""
}
