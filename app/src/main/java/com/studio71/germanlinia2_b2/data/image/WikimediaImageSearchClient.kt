package com.studio71.germanlinia2_b2.data.image

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.studio71.germanlinia2_b2.data.local.VocabularyEntity
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Locale

data class MeaningImageLookupResult(
    val imageUrl: String?,
    val queryUsed: String
)

/**
 * Resolves a meaning-oriented image URL for one word from Wikimedia Commons.
 */
class WikimediaImageSearchClient {

    private val api: WikimediaApi

    init {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "GermanLiniA2_B2/1.0 (meaning-image-lookup)")
                    .build()
                chain.proceed(request)
            }
            .build()

        api = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WikimediaApi::class.java)
    }

    suspend fun findMeaningImage(word: VocabularyEntity): MeaningImageLookupResult {
        val queries = composeMeaningQueries(word)
        var hadSuccessfulResponse = false
        var lastError: Throwable? = null

        for (query in queries) {
            val response = try {
                api.searchImages(search = query)
            } catch (t: Throwable) {
                lastError = t
                continue
            }

            hadSuccessfulResponse = true
            val best = pickBestImage(word, response)
            if (best != null) return MeaningImageLookupResult(imageUrl = best, queryUsed = query)
        }

        if (!hadSuccessfulResponse && lastError != null) throw lastError
        return MeaningImageLookupResult(imageUrl = null, queryUsed = queries.firstOrNull().orEmpty())
    }

    private fun pickBestImage(word: VocabularyEntity, response: WikimediaQueryResponse): String? {
        val pages = response.query?.pages?.values.orEmpty()
        if (pages.isEmpty()) return null

        val wordToken = normalize(word.word)
        val meaningTokens = meaningKeywords(word)

        return pages
            .mapNotNull { page ->
                val url = page.imageInfo?.firstOrNull()?.url ?: return@mapNotNull null
                if (!isProbablyRenderableImage(url)) return@mapNotNull null
                val title = page.title.orEmpty()
                val score = scoreCandidate(title = title, url = url, wordToken = wordToken, meaningTokens = meaningTokens)
                if (score <= 0) null else Candidate(url = url, score = score)
            }
            .maxByOrNull { it.score }
            ?.url
    }

    private fun composeMeaningQueries(word: VocabularyEntity): List<String> {
        val baseWord = word.word.trim()
        val nounForm = word.displayWord.trim().takeIf { it != baseWord }.orEmpty()
        val meaning = meaningKeywords(word).take(4).joinToString(" ")

        val primary = buildString {
            append(baseWord)
            if (meaning.isNotBlank()) append(" ").append(meaning)
        }.trim()

        val queries = linkedSetOf<String>()
        if (primary.isNotBlank()) queries += primary
        if (nounForm.isNotBlank()) queries += nounForm

        // Concept-photo fallback for abstract words: still photo-oriented, not icon-first.
        if (meaning.isNotBlank()) {
            queries += "$baseWord $meaning konzept"
        }

        return queries.filter { it.isNotBlank() }
    }

    private fun meaningKeywords(word: VocabularyEntity): List<String> {
        if (word.germanMeaning.isBlank()) return emptyList()
        return word.germanMeaning
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-zA-ZäöüÄÖÜß\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 3 && it !in GERMAN_STOP_WORDS }
            .distinct()
    }

    private fun scoreCandidate(
        title: String,
        url: String,
        wordToken: String,
        meaningTokens: List<String>
    ): Int {
        val normalizedTitle = normalize(title)
        val normalizedUrl = normalize(url)
        val urlLower = url.substringBefore('?').lowercase(Locale.ROOT)

        var score = 0
        if (wordToken.isNotBlank() && normalizedTitle.contains(wordToken)) score += 6

        meaningTokens.take(3).forEach { token ->
            if (normalizedTitle.contains(normalize(token))) score += 2
        }

        if (urlLower.endsWith(".jpg") || urlLower.endsWith(".jpeg") || urlLower.endsWith(".png") || urlLower.endsWith(".webp")) {
            score += 3
        }
        if (urlLower.endsWith(".svg")) score -= 4

        if (BAD_VISUAL_TOKENS.any { normalizedTitle.contains(it) || normalizedUrl.contains(it) }) {
            score -= 8
        }
        return score
    }

    private fun normalize(value: String): String =
        value.lowercase(Locale.ROOT)
            .replace(Regex("[^a-zA-Z0-9äöüÄÖÜß]"), "")

    private fun isProbablyRenderableImage(url: String): Boolean {
        val clean = url.substringBefore('?').lowercase(Locale.ROOT)
        return clean.endsWith(".jpg") ||
            clean.endsWith(".jpeg") ||
            clean.endsWith(".png") ||
            clean.endsWith(".webp") ||
            clean.endsWith(".svg")
    }

    private data class Candidate(val url: String, val score: Int)

    private interface WikimediaApi {
        @GET("w/api.php")
        suspend fun searchImages(
            @Query("action") action: String = "query",
            @Query("format") format: String = "json",
            @Query("generator") generator: String = "search",
            @Query("gsrnamespace") namespace: Int = 6,
            @Query("gsrsearch") search: String,
            @Query("gsrlimit") limit: Int = 12,
            @Query("prop") prop: String = "imageinfo",
            @Query("iiprop") iiprop: String = "url"
        ): WikimediaQueryResponse
    }

    private data class WikimediaQueryResponse(
        @param:Json(name = "query") val query: WikimediaQueryContainer?
    )

    private data class WikimediaQueryContainer(
        @param:Json(name = "pages") val pages: Map<String, WikimediaPage>?
    )

    private data class WikimediaPage(
        @param:Json(name = "title") val title: String?,
        @param:Json(name = "imageinfo") val imageInfo: List<WikimediaImageInfo>?
    )

    private data class WikimediaImageInfo(
        @param:Json(name = "url") val url: String?
    )

    private companion object {
        const val BASE_URL = "https://commons.wikimedia.org/"

        val BAD_VISUAL_TOKENS = listOf(
            "icon", "logo", "piktogramm", "pictogram", "symbol", "coatofarms", "flag"
        )

        val GERMAN_STOP_WORDS = setOf(
            "der", "die", "das", "und", "oder", "ein", "eine", "einer", "einem", "einen",
            "zu", "mit", "von", "für", "ist", "sind", "wird", "werden", "den", "dem", "des",
            "auf", "im", "in", "am", "an", "als", "bei", "durch", "zur", "zum", "man"
        )
    }
}


