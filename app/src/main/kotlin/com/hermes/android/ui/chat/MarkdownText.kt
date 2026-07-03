package com.hermes.android.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Lightweight markdown renderer for chat messages.
 * Supports: bold (**text**), italic (*text*), inline code (`text`),
 * code blocks (```), headers (#, ##, ###), bullet lists (-, *).
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val codeBlockBackground = MaterialTheme.colorScheme.surfaceVariant
    val codeStyle = MaterialTheme.typography.bodySmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Column(modifier = modifier) {
        val lines = text.split("\n")
        var inCodeBlock = false
        var codeBlockContent = StringBuilder()

        for (line in lines) {
            when {
                line.trimStart().startsWith("```") -> {
                    if (inCodeBlock) {
                        // End code block - render accumulated content
                        if (codeBlockContent.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(codeBlockBackground)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = codeBlockContent.toString().trimEnd(),
                                    style = codeStyle
                                )
                            }
                            codeBlockContent = StringBuilder()
                        }
                        inCodeBlock = false
                    } else {
                        inCodeBlock = true
                    }
                }
                inCodeBlock -> {
                    codeBlockContent.appendLine(line)
                }
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# "),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                    val bulletText = line.trimStart().removePrefix("- ").removePrefix("* ")
                    Row(modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)) {
                        Text(text = "\u2022  ", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = parseInlineMarkdown(bulletText),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {
                    Text(
                        text = parseInlineMarkdown(line),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }

        // Flush trailing code block
        if (inCodeBlock && codeBlockContent.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(codeBlockBackground)
                    .padding(12.dp)
            ) {
                Text(
                    text = codeBlockContent.toString().trimEnd(),
                    style = codeStyle
                )
            }
        }
    }
}

/**
 * Parse inline markdown: bold (**text**), italic (*text*), inline code (`text`).
 */
private fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // Bold: **text**
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Inline code: `text`
                text.startsWith("`", i) -> {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            background = androidx.compose.ui.graphics.Color(0xFFE0E0E0)
                        )) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Italic: *text*
                text.startsWith("*", i) && !text.startsWith("**", i) -> {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1 && end != i + 1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
