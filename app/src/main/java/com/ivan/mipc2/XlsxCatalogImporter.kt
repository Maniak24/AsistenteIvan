package com.ivan.mipc2

import android.content.ContentResolver
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.util.zip.ZipInputStream

object XlsxCatalogImporter {

    fun read(
        resolver: ContentResolver,
        uri: Uri
    ): List<List<String>> {

        val entries = mutableMapOf<String, ByteArray>()

        resolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break

                    if (!entry.isDirectory) {
                        entries[entry.name] = zip.readBytes()
                    }
                }
            }
        } ?: throw Exception("No se pudo abrir el Excel.")

        val sharedStrings =
            parseSharedStrings(entries["xl/sharedStrings.xml"])

        val sheet =
            entries["xl/worksheets/sheet1.xml"]
                ?: throw Exception("No encontré la primera hoja del Excel.")

        return parseSheet(sheet, sharedStrings)
    }

    private fun parseSharedStrings(
        data: ByteArray?
    ): List<String> {

        if (data == null) return emptyList()

        val result = mutableListOf<String>()
        val parser = newParser(data)

        var current = ""
        var insideSi = false

        while (parser.next() != XmlPullParser.END_DOCUMENT) {

            when (parser.eventType) {

                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "si" -> {
                            insideSi = true
                            current = ""
                        }

                        "t" -> {
                            if (insideSi) {
                                current += parser.nextText()
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "si" && insideSi) {
                        result.add(current)
                        insideSi = false
                    }
                }
            }
        }

        return result
    }

    private fun parseSheet(
        data: ByteArray,
        sharedStrings: List<String>
    ): List<List<String>> {

        val rows = mutableListOf<List<String>>()
        val parser = newParser(data)

        var currentRow = mutableMapOf<Int, String>()
        var currentCellColumn = 0
        var currentCellType = ""
        var currentValue = ""

        while (parser.next() != XmlPullParser.END_DOCUMENT) {

            when (parser.eventType) {

                XmlPullParser.START_TAG -> {

                    when (parser.name) {

                        "row" -> {
                            currentRow = mutableMapOf()
                        }

                        "c" -> {
                            val ref =
                                parser.getAttributeValue(null, "r") ?: "A1"

                            currentCellColumn =
                                columnIndex(
                                    ref.takeWhile { it.isLetter() }
                                )

                            currentCellType =
                                parser.getAttributeValue(null, "t") ?: ""

                            currentValue = ""
                        }

                        "v" -> {
                            currentValue = parser.nextText()
                        }

                        "t" -> {
                            if (currentCellType == "inlineStr") {
                                currentValue = parser.nextText()
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {

                    when (parser.name) {

                        "c" -> {
                            var value = currentValue

                            if (currentCellType == "s") {
                                value =
                                    sharedStrings
                                        .getOrNull(value.toIntOrNull() ?: -1)
                                        ?: ""
                            }

                            currentRow[currentCellColumn] = value
                        }

                        "row" -> {
                            if (currentRow.isNotEmpty()) {

                                val max =
                                    currentRow.keys.maxOrNull() ?: 0

                                rows.add(
                                    (0..max).map {
                                        currentRow[it] ?: ""
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        return rows
    }

    private fun columnIndex(value: String): Int {

        var result = 0

        for (char in value.uppercase()) {
            if (char !in 'A'..'Z') break
            result = result * 26 + (char - 'A' + 1)
        }

        return result - 1
    }

    private fun newParser(data: ByteArray): XmlPullParser {
        return Xml.newPullParser().apply {
            setInput(
                data.inputStream().bufferedReader(Charsets.UTF_8)
            )
        }
    }
}
