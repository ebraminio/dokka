package org.jetbrains.dokka.xml

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DefaultExtra
import org.jetbrains.dokka.DokkaConsoleLogger
import org.jetbrains.dokka.Model.DocumentationNode
import org.jetbrains.dokka.Model.dfs
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.transformers.PageNodeTransformer
import javax.xml.bind.annotation.XmlList

class XmlPlugin : DokkaPlugin() {
    val transformer by extending {
        CoreExtensions.pageTransformer with XmlTransformer
    }
}

object XmlTransformer : PageNodeTransformer {
    enum class XMLKind : Kind {
        Main, XmlList
    }

    override fun invoke(input: ModulePageNode, dokkaContext: DokkaContext): ModulePageNode =
        input.transformPageNodeTree { node ->
            if (node !is ClassPageNode) node
            else {
                val refs =
                    node.documentationNode?.extra?.filterIsInstance<DefaultExtra>()?.filter { it.key == "@attr ref" }
                        .orEmpty()
                val elementsToAdd = mutableListOf<DocumentationNode>()

                refs.forEach { ref ->
                    val toFind = DRI.from(ref.value)
                    input.documentationNode?.dfs { it.dri == toFind }?.let { elementsToAdd.add(it) }
                }
                val platformData = node.platforms().toSet()
                val refTable = DefaultPageContentBuilder.group(
                    node.dri,
                    platformData,
                    XMLKind.XmlList,
                    //Following parameters will soon be drawn from context, so we can leave them like this for the time being
                    MarkdownToContentConverter(DokkaConsoleLogger),
                    DokkaConsoleLogger
                ) {
                    block("XML Attributes", 2, XMLKind.XmlList, elementsToAdd, platformData) { element ->
                        link(element.dri, XMLKind.XmlList) {
                            text(element.name ?: "<unnamed>", XMLKind.Main)
                        }
                        text(element.briefDocstring, XMLKind.XmlList)
                    }
                }

                val content = node.content as ContentGroup
                val children = (node.content as ContentGroup).children
                node.modified(content = content.copy(children = children + refTable))
            }
    }

    private fun PageNode.platforms() = this.content.platforms.toList()
}