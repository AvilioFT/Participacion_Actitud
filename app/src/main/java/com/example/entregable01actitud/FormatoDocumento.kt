package com.example.entregable01actitud

import org.json.JSONArray
import org.json.JSONObject
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object FormatoDocumento {

    data class Formato(
        val id: String,
        val etiqueta: String,
        val mime: String,
        val extension: String
    )

    private val BASE = listOf(
        Formato("txt", "TXT", "text/plain", "txt"),
        Formato("json", "JSON", "application/json", "json"),
        Formato("xml", "XML", "text/xml", "xml"),
        Formato("html", "HTML", "text/html", "html"),
        Formato("md", "Markdown", "text/markdown", "md"),
        Formato(
            "docx",
            "Word",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "docx"
        ),
        Formato(
            "xlsx",
            "Excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "xlsx"
        )
    )

    private val CSV = Formato("csv", "CSV", "text/csv", "csv")

    fun disponibles(tieneCsv: Boolean): List<Formato> {
        val lista = mutableListOf(BASE[0])
        if (tieneCsv) lista.add(CSV)
        lista.addAll(BASE.subList(1, BASE.size))
        return lista
    }

    fun generar(
        formatoId: String,
        api: String,
        fecha: String,
        contenido: String,
        csv: String,
        imagen: ByteArray? = null
    ): ByteArray {
        val tabla = if (csv.isBlank()) emptyList() else parseCsv(csv)
        return when (formatoId) {
            "csv" -> csv.toByteArray(Charsets.UTF_8)
            "json" -> aJson(api, fecha, contenido, tabla).toByteArray(Charsets.UTF_8)
            "xml" -> aXml(api, fecha, contenido, tabla).toByteArray(Charsets.UTF_8)
            "html" -> aHtml(api, fecha, contenido, tabla).toByteArray(Charsets.UTF_8)
            "md" -> aMarkdown(api, fecha, contenido, tabla).toByteArray(Charsets.UTF_8)
            "docx" -> aDocx(api, fecha, contenido, tabla, imagen)
            "xlsx" -> aXlsx(contenido, tabla)
            else -> contenido.toByteArray(Charsets.UTF_8)
        }
    }

    fun parseCsv(csv: String): List<List<String>> {
        return csv.trim().split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { linea -> linea.split(",").map { it.trim() } }
    }

    private fun escXml(texto: String): String {
        return texto
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    private fun escHtml(texto: String): String {
        return escXml(texto).replace("\n", "<br/>")
    }

    private fun aJson(
        api: String,
        fecha: String,
        contenido: String,
        tabla: List<List<String>>
    ): String {
        val objeto = JSONObject()
        objeto.put("api", api)
        objeto.put("fecha", fecha)
        objeto.put("contenido", contenido)
        if (tabla.size > 1) {
            val cabecera = tabla[0]
            val filas = JSONArray()
            for (i in 1 until tabla.size) {
                val fila = JSONObject()
                for (j in cabecera.indices) {
                    val valor = tabla[i].getOrNull(j) ?: ""
                    fila.put(cabecera[j], valor)
                }
                filas.put(fila)
            }
            objeto.put("tabla", filas)
        }
        return objeto.toString(2)
    }

    private fun aXml(
        api: String,
        fecha: String,
        contenido: String,
        tabla: List<List<String>>
    ): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<documento>\n")
        sb.append("  <api>${escXml(api)}</api>\n")
        sb.append("  <fecha>${escXml(fecha)}</fecha>\n")
        sb.append("  <contenido>${escXml(contenido)}</contenido>\n")
        if (tabla.size > 1) {
            sb.append("  <tabla>\n")
            val cabecera = tabla[0]
            for (i in 1 until tabla.size) {
                sb.append("    <fila>\n")
                for (j in cabecera.indices) {
                    val nombre = escXml(
                        cabecera[j].ifEmpty { "col$j" }
                            .replace(Regex("[^a-zA-Z0-9_]"), "_")
                    )
                    val valor = escXml(tabla[i].getOrNull(j) ?: "")
                    sb.append("      <$nombre>$valor</$nombre>\n")
                }
                sb.append("    </fila>\n")
            }
            sb.append("  </tabla>\n")
        }
        sb.append("</documento>\n")
        return sb.toString()
    }

    private fun aHtml(
        api: String,
        fecha: String,
        contenido: String,
        tabla: List<List<String>>
    ): String {
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html>\n<html lang=\"es\">\n<head>\n")
        sb.append("<meta charset=\"UTF-8\"/>\n")
        sb.append("<title>${escXml(api)}</title>\n")
        sb.append("</head>\n<body>\n")
        sb.append("<h1>${escXml(api)}</h1>\n")
        sb.append("<p><strong>Fecha:</strong> ${escXml(fecha)}</p>\n")
        sb.append("<pre>${escHtml(contenido)}</pre>\n")
        if (tabla.size > 1) {
            sb.append("<table border=\"1\" cellpadding=\"6\">\n")
            sb.append("<thead><tr>")
            for (celda in tabla[0]) sb.append("<th>${escXml(celda)}</th>")
            sb.append("</tr></thead>\n<tbody>\n")
            for (i in 1 until tabla.size) {
                sb.append("<tr>")
                for (j in tabla[0].indices) {
                    sb.append("<td>${escXml(tabla[i].getOrNull(j) ?: "")}</td>")
                }
                sb.append("</tr>\n")
            }
            sb.append("</tbody>\n</table>\n")
        }
        sb.append("</body>\n</html>\n")
        return sb.toString()
    }

    private fun aMarkdown(
        api: String,
        fecha: String,
        contenido: String,
        tabla: List<List<String>>
    ): String {
        val sb = StringBuilder()
        sb.append("# $api\n\n")
        sb.append("Fecha: $fecha\n\n")
        sb.append("```\n$contenido\n```\n")
        if (tabla.size > 1) {
            sb.append("\n## Tabla\n\n")
            sb.append(tabla[0].joinToString(" | ", "| ", " |") + "\n")
            sb.append(tabla[0].joinToString(" | ", "| ", " |") { "---" } + "\n")
            for (i in 1 until tabla.size) {
                val fila = tabla[0].indices.map { tabla[i].getOrNull(it) ?: "" }
                sb.append(fila.joinToString(" | ", "| ", " |") + "\n")
            }
        }
        return sb.toString()
    }

    private fun parrafoDocx(texto: String, negrita: Boolean, tamano: Int): String {
        val props = if (negrita)
            "<w:rPr><w:b/><w:sz w:val=\"${tamano * 2}\"/></w:rPr>"
        else
            "<w:rPr><w:sz w:val=\"${tamano * 2}\"/></w:rPr>"
        return "<w:p><w:r>$props<w:t xml:space=\"preserve\">" +
                "${escXml(texto)}</w:t></w:r></w:p>"
    }

    private fun aDocx(
        api: String,
        fecha: String,
        contenido: String,
        tabla: List<List<String>>,
        imagen: ByteArray? = null
    ): ByteArray {
        val cuerpo = StringBuilder()
        cuerpo.append(parrafoDocx(api, true, 16))
        cuerpo.append(parrafoDocx("Fecha: $fecha", false, 11))
        for (linea in contenido.split("\n")) {
            cuerpo.append(parrafoDocx(linea, false, 11))
        }

        val datosImagen = prepararImagen(imagen)
        if (datosImagen != null) {
            cuerpo.append(parrafoImagenDocx(datosImagen))
        }

        if (tabla.size > 1) {
            cuerpo.append(parrafoDocx("Tabla", true, 13))
            cuerpo.append("<w:tbl><w:tblPr><w:tblW w:w=\"5000\" w:type=\"pct\"/></w:tblPr>")
            cuerpo.append("<w:tblGrid>")
            for (j in tabla[0].indices) cuerpo.append("<w:gridCol/>")
            cuerpo.append("</w:tblGrid>")
            for ((indice, fila) in tabla.withIndex()) {
                cuerpo.append("<w:tr>")
                for (j in tabla[0].indices) {
                    val texto = fila.getOrNull(j) ?: ""
                    val celda = if (indice == 0)
                        parrafoDocx(texto, true, 11)
                    else
                        parrafoDocx(texto, false, 11)
                    cuerpo.append("<w:tc><w:tcPr><w:tcW w:w=\"0\" w:type=\"auto\"/></w:tcPr>$celda</w:tc>")
                }
                cuerpo.append("</w:tr>")
            }
            cuerpo.append("</w:tbl>")
        }
        cuerpo.append("<w:sectPr><w:pgSz w:w=\"12240\" w:h=\"15840\"/></w:sectPr>")

        val documento =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" " +
                    "xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" " +
                    "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                    "<w:body>$cuerpo</w:body></w:document>"

        val tiposBase =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                    "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                    "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"

        val rels =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>" +
                    "</Relationships>"

        if (datosImagen == null) {
            return empaquetarZip(
                listOf(
                    "[Content_Types].xml" to tiposBase + "</Types>",
                    "_rels/.rels" to rels,
                    "word/document.xml" to documento
                )
            )
        }

        val tipos = tiposBase +
                "<Default Extension=\"${datosImagen.extension}\" ContentType=\"${datosImagen.mime}\"/>" +
                "<Override PartName=\"/word/_rels/document.xml.rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "</Types>"

        val relsDoc =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"media/imagen1.${datosImagen.extension}\"/>" +
                    "</Relationships>"

        val entradas = mutableListOf(
            "[Content_Types].xml" to tipos,
            "_rels/.rels" to rels,
            "word/document.xml" to documento,
            "word/_rels/document.xml.rels" to relsDoc
        )

        return empaquetarZipBytes(
            entradas,
            listOf(
                "word/media/imagen1.${datosImagen.extension}" to datosImagen.bytes
            )
        )
    }

    private data class ImagenDocx(
        val bytes: ByteArray,
        val extension: String,
        val mime: String,
        val anchoEmu: Long,
        val altoEmu: Long
    )

    private fun prepararImagen(
        imagen: ByteArray?
    ): ImagenDocx? {
        if (imagen == null || imagen.size < 100) return null

        val esJpg = imagen.size > 2 &&
                imagen[0] == 0xFF.toByte() &&
                imagen[1] == 0xD8.toByte()

        val opciones = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(imagen, 0, imagen.size, opciones)

        val ancho = opciones.outWidth
        val alto = opciones.outHeight
        if (ancho <= 0 || alto <= 0) return null

        val emuPorPixel = 9525L
        val maxAncho = 5486400L
        var cx = ancho * emuPorPixel
        var cy = alto * emuPorPixel
        if (cx > maxAncho) {
            cy = cy * maxAncho / cx
            cx = maxAncho
        }

        return ImagenDocx(
            imagen,
            if (esJpg) "jpg" else "png",
            if (esJpg) "image/jpeg" else "image/png",
            cx,
            cy
        )
    }

    private fun parrafoImagenDocx(
        imagen: ImagenDocx
    ): String {
        return "<w:p><w:r><w:drawing>" +
                "<wp:inline distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\">" +
                "<wp:extent cx=\"${imagen.anchoEmu}\" cy=\"${imagen.altoEmu}\"/>" +
                "<wp:docPr id=\"1\" name=\"Imagen\"/>" +
                "<a:graphic xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">" +
                "<a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">" +
                "<pic:pic xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">" +
                "<pic:nvPicPr><pic:cNvPr id=\"1\" name=\"imagen.${imagen.extension}\"/><pic:cNvPicPr/></pic:nvPicPr>" +
                "<pic:blipFill><a:blip r:embed=\"rId1\"/>" +
                "<a:stretch><a:fillRect/></a:stretch></pic:blipFill>" +
                "<pic:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/>" +
                "<a:ext cx=\"${imagen.anchoEmu}\" cy=\"${imagen.altoEmu}\"/>" +
                "</a:xfrm><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></pic:spPr>" +
                "</pic:pic></a:graphicData></a:graphic>" +
                "</wp:inline></w:drawing></w:r></w:p>"
    }

    private fun nombreColumna(indice: Int): String {
        var i = indice
        var nombre = ""
        do {
            nombre = ('A'.code + (i % 26)).toChar() + nombre
            i = i / 26 - 1
        } while (i >= 0)
        return nombre
    }

    private fun celdaXlsx(
        fila: Int,
        columna: Int,
        valor: String,
        encabezado: Boolean
    ): String {
        val ref = "${nombreColumna(columna)}$fila"
        val numero = valor.toDoubleOrNull()
        return if (numero != null && valor.isNotBlank()) {
            if (encabezado)
                "<c r=\"$ref\" s=\"1\"><v>$valor</v></c>"
            else
                "<c r=\"$ref\"><v>$valor</v></c>"
        } else {
            if (encabezado)
                "<c r=\"$ref\" s=\"1\" t=\"inlineStr\"><is><t>${escXml(valor)}</t></is></c>"
            else
                "<c r=\"$ref\" t=\"inlineStr\"><is><t>${escXml(valor)}</t></is></c>"
        }
    }

    private fun aXlsx(
        contenido: String,
        tabla: List<List<String>>
    ): ByteArray {
        val datos: List<List<String>> = if (tabla.size > 1) {
            tabla
        } else {
            listOf(listOf("Contenido")) +
                    contenido.split("\n").map { listOf(it) }
        }

        val filasXml = StringBuilder()
        for ((i, fila) in datos.withIndex()) {
            filasXml.append("<row r=\"${i + 1}\">")
            for (j in datos[0].indices) {
                filasXml.append(
                    celdaXlsx(
                        i + 1, j,
                        fila.getOrNull(j) ?: "",
                        i == 0
                    )
                )
            }
            filasXml.append("</row>")
        }

        val hoja =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
                    "<sheetData>$filasXml</sheetData></worksheet>"

        val libro =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
                    "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                    "<sheets><sheet name=\"Datos\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>"

        val relsLibro =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                    "</Relationships>"

        val estilos =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
                    "<fonts count=\"2\">" +
                    "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
                    "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
                    "</fonts>" +
                    "<fills count=\"2\">" +
                    "<fill><patternFill patternType=\"none\"/></fill>" +
                    "<fill><patternFill patternType=\"gray125\"/></fill>" +
                    "</fills>" +
                    "<borders count=\"1\">" +
                    "<border><left/><right/><top/><bottom/><diagonal/></border>" +
                    "</borders>" +
                    "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
                    "<cellXfs count=\"2\">" +
                    "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
                    "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>" +
                    "</cellXfs></styleSheet>"

        val tipos =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                    "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                    "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                    "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                    "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
                    "</Types>"

        val rels =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                    "</Relationships>"

        return empaquetarZip(
            listOf(
                "[Content_Types].xml" to tipos,
                "_rels/.rels" to rels,
                "xl/workbook.xml" to libro,
                "xl/_rels/workbook.xml.rels" to relsLibro,
                "xl/worksheets/sheet1.xml" to hoja,
                "xl/styles.xml" to estilos
            )
        )
    }

    private fun empaquetarZip(
        entradas: List<Pair<String, String>>
    ): ByteArray {
        return empaquetarZipBytes(
            entradas,
            emptyList()
        )
    }

    private fun empaquetarZipBytes(
        textos: List<Pair<String, String>>,
        binarios: List<Pair<String, ByteArray>>
    ): ByteArray {
        val buffer = ByteArrayOutputStream()
        ZipOutputStream(buffer).use { zip ->
            for ((nombre, contenido) in textos) {
                zip.putNextEntry(ZipEntry(nombre))
                zip.write(contenido.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            for ((nombre, bytes) in binarios) {
                zip.putNextEntry(ZipEntry(nombre))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return buffer.toByteArray()
    }

    fun formatearMonto(valor: Double): String {
        return String.format(Locale.US, "%.2f", valor)
    }
}
