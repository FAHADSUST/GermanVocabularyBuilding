package com.studio71.germanlinia2_b2.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.studio71.germanlinia2_b2.data.local.VocabularyEntity

/**
 * Common colors for German articles to aid memorization:
 * - Masculine (der) -> Blue
 * - Feminine (die) -> Red / Pink
 * - Neutral (das) -> Green
 */
object ArticleColors {
    val BlueLight = Color(0xFF1976D2)
    val BlueDark = Color(0xFF64B5F6)

    val RedLight = Color(0xFFD32F2F)
    val RedDark = Color(0xFFEF9A9A)

    val GreenLight = Color(0xFF388E3C)
    val GreenDark = Color(0xFF81C784)

    @Composable
    fun getArticleColor(article: String, isDark: Boolean = isSystemInDarkTheme()): Color? {
        return when (article.trim().lowercase()) {
            "der" -> if (isDark) BlueDark else BlueLight
            "die" -> if (isDark) RedDark else RedLight
            "das" -> if (isDark) GreenDark else GreenLight
            else -> null
        }
    }
}

/**
 * Returns an AnnotatedString with the article (der/die/das) formatted in its characteristic color and bold.
 */
@Composable
fun VocabularyEntity.getAnnotatedDisplayWord(): AnnotatedString {
    val cleanArticle = article.trim()
    if (cleanArticle.isBlank()) {
        return AnnotatedString(word)
    }
    val articleColor = ArticleColors.getArticleColor(cleanArticle)
    return buildAnnotatedString {
        if (articleColor != null) {
            withStyle(SpanStyle(color = articleColor, fontWeight = FontWeight.Bold)) {
                append(cleanArticle)
            }
        } else {
            append(cleanArticle)
        }
        append(" ")
        append(word)
    }
}

/**
 * Parses and highlights the article at the beginning of a raw display string (e.g. "die Wohnung" -> highlighted "die").
 */
@Composable
fun String.highlightGermanArticles(): AnnotatedString {
    val trimmed = this.trim()
    val parts = trimmed.split(Regex("\\s+"), limit = 2)
    if (parts.size < 2) {
        val articleColor = ArticleColors.getArticleColor(trimmed)
        if (articleColor != null) {
            return buildAnnotatedString {
                withStyle(SpanStyle(color = articleColor, fontWeight = FontWeight.Bold)) {
                    append(trimmed)
                }
            }
        }
        return AnnotatedString(this)
    }

    val firstWord = parts[0]
    val rest = parts[1]
    val articleColor = ArticleColors.getArticleColor(firstWord)
    if (articleColor != null) {
        return buildAnnotatedString {
            withStyle(SpanStyle(color = articleColor, fontWeight = FontWeight.Bold)) {
                append(firstWord)
            }
            append(" ")
            append(rest)
        }
    }
    return AnnotatedString(this)
}

