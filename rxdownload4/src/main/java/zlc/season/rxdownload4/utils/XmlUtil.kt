package zlc.season.rxdownload4.utils

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

val builderFactory by lazy { DocumentBuilderFactory.newInstance() }

fun parse(filePath: String): Document? {
    var document: Document? = null
    try {
        val builder = builderFactory.newDocumentBuilder()
        document = builder.parse(File(filePath))
    } catch (e: Exception) {
    }
    return document
}


fun main(args: Array<String>) {
    val parser = DOMParser()
    val document = parser.parse("books.xml")
    //get root element
    val rootElement = document!!.documentElement

    //traverse child elements
    val nodes = rootElement.childNodes
    for (i in 0 until nodes.length) {
        val node = nodes.item(i)
        if (node.nodeType == Node.ELEMENT_NODE) {
            val child = node as Element
            //process child element
        }
    }

    val nodeList = rootElement.getElementsByTagName("book")
    if (nodeList != null) {
        for (i in 0 until nodeList.length) {
            val element = nodeList.item(i) as Element
            val id = element.getAttribute("id")
        }
    }
}