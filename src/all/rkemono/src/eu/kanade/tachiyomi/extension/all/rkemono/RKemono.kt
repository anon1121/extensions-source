package eu.kanade.tachiyomi.extension.all.rkemono

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class RKemono() : ParsedHttpSource() {
    override val baseUrl = "https://kemono.su"
    override val lang = "all"
    override val name = "RKemono"
    override val supportsLatest = true

    // Latest
    override fun latestUpdatesFromElement(element: Element): SManga {
        val manga = SManga.create()
        manga.thumbnail_url = element.select("div.post-card__image-container > img.post-card__image").attr("abs:src")
        manga.title = element.select("header.post-card__header").text()
        manga.setUrlWithoutDomain(element.select("article > a").attr("abs:href"))
        return manga
    }

    override fun latestUpdatesNextPageSelector() {
        return "#paginator-top .pagination-button-after-current:not(.pagination-button-disabled)"
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/api/v1/posts?o=${(($page - 1) * 50)}")
    }

    override fun latestUpdatesSelector() {
        return ".card-list__items > article.post-card"
    }

    // Popular
    override fun popularMangaFromElement(element: Element) = latestUpdatesFromElement(element)
    override fun popularMangaNextPageSelector() = latestUpdatesNextPageSelector()
    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/api/v1/posts/popular?o=${(($page - 1) * 50)}")
    }
    override fun popularMangaSelector() = latestUpdatesSelector()

    // Search

    override fun searchMangaFromElement(element: Element) = latestUpdatesFromElement(element)
    override fun searchMangaNextPageSelector() = latestUpdatesNextPageSelector()
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val tagFilter = filters.findInstance<TagFilter>()!!
        return when {
            query.isNotEmpty() -> GET("$baseUrl/api/v1/posts?o=${((page - 1) * 50)}&q=$query")
            else -> popularMangaRequest(page)
        }
    }
    override fun searchMangaSelector() = latestUpdatesSelector()

    // Details
    override fun mangaDetailsParse(document: Document): SManga {
        val manga = SManga.create()
        manga.title = document.select("header.post__header > div.post__info > h1 > span").first().text()
        manga.description = document.select(".video-title h1").text().trim()
        val genres = mutableListOf<String>()
        document.select("section#post-tags > div").first()!!.select("a").forEach {
            genres.add(it.text())
        }
        manga.genre = genres.joinToString(", ")
        return manga
    }

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = SChapter.create()
        chapter.chapter_number = 0F
        chapter.name = element.select("title").text()
        return chapter
    }

    override fun chapterListSelector() = "html"

    // Pages

    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()
        document.select("div.post__files a").forEach {
            val itUrl = it.attr("href")
            pages.add(Page(pages.size, "", itUrl))
        }
        return pages
    }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException()

    // Filters
    override fun getFilterList(): FilterList = FilterList(
        Filter.Header("NOTE: Ignored if using text search!"),
        Filter.Separator(),
        TagFilter(),
    )

    class TagFilter : Filter.Text("Tag ID")

    private inline fun <reified T> Iterable<*>.findInstance() = find { it is T } as? T
}
