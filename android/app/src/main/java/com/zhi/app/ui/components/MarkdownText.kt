package com.zhi.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text as MarkdownNodeText
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser

private val markdownParser = Parser.builder()
    .extensions(listOf(TablesExtension.create()))
    .build()

private fun children(node: Node): Sequence<Node> =
    generateSequence(node.firstChild) { it.next }

private fun safeLink(url: String): Boolean = runCatching {
    val uri = java.net.URI(url)
    uri.scheme?.lowercase() in listOf("http", "https") &&
        !uri.host.isNullOrBlank() &&
        uri.userInfo == null
}.getOrDefault(false)

private fun AnnotatedString.Builder.appendInline(node: Node, linkColor: Color) {
    when (node) {
        is MarkdownNodeText -> append(node.literal)
        is SoftLineBreak -> append(" ")
        is HardLineBreak -> append("\n")
        is Code -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
            append(node.literal)
        }
        is StrongEmphasis -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            children(node).forEach { appendInline(it, linkColor) }
        }
        is Emphasis -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            children(node).forEach { appendInline(it, linkColor) }
        }
        is Link -> if (safeLink(node.destination)) {
            withLink(
                androidx.compose.ui.text.LinkAnnotation.Url(
                    node.destination,
                    TextLinkStyles(style = SpanStyle(color = linkColor))
                )
            ) {
                children(node).forEach { appendInline(it, linkColor) }
            }
        } else {
            children(node).forEach { appendInline(it, linkColor) }
        }
        is HtmlInline -> Unit
        else -> children(node).forEach { appendInline(it, linkColor) }
    }
}

/** Renders the GitHub-flavoured Markdown used in release notes. */
@Composable
fun MarkdownText(content: String, modifier: Modifier = Modifier) {
    val document = remember(content) { markdownParser.parse(content) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        children(document).forEach { MarkdownBlock(it) }
    }
}

@Composable
private fun MarkdownBlock(node: Node) {
    val linkColor = MaterialTheme.colorScheme.primary
    when (node) {
        is Paragraph, is Heading -> {
            val text = AnnotatedString.Builder().apply {
                children(node).forEach { appendInline(it, linkColor) }
            }.toAnnotatedString()
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                style = if (node is Heading) {
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                } else {
                    MaterialTheme.typography.bodyMedium
                }
            )
        }
        is BulletList, is OrderedList -> Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            children(node).forEachIndexed { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (node is OrderedList) {
                            "${node.markerStartNumber + index}."
                        } else {
                            "•"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        children(item).forEach { MarkdownBlock(it) }
                    }
                }
            }
        }
        is BlockQuote -> Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.small
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                children(node).forEach { MarkdownBlock(it) }
            }
        }
        is FencedCodeBlock, is IndentedCodeBlock -> {
            val code = if (node is FencedCodeBlock) {
                node.literal
            } else {
                (node as IndentedCodeBlock).literal
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = code.trimEnd(),
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        is ThematicBreak -> HorizontalDivider()
        is TableBlock -> Column(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            children(node).forEach { section ->
                children(section).forEach { row ->
                    Row {
                        children(row).forEach { cell ->
                            Text(
                                text = AnnotatedString.Builder().apply {
                                    children(cell).forEach { appendInline(it, linkColor) }
                                }.toAnnotatedString(),
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if ((cell as? TableCell)?.isHeader == true) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Normal
                                }
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
        is HtmlBlock -> Unit
        else -> children(node).forEach { MarkdownBlock(it) }
    }
}
