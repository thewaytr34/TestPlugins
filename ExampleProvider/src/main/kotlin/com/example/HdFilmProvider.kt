package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class HdFilmProvider : MainAPI() {
    override var name = "HDFilmIzle"
    override var mainUrl = "https://www.hdfilmizle.best"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/sayfa/$page/").document
        val movies = document.select("div.poster, article.item").mapNotNull { toSearchResult(it) }
        return HomePageResponse(listOf(HomePageList("Yeni Filmler", movies)))
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("div.poster, article.item").mapNotNull { toSearchResult(it) }
    }

    private fun toSearchResult(element: Element): SearchResponse? {
        val title = element.selectFirst(".title, h2, h3")?.text() ?: return null
        val href = fixUrlNull(element.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(element.selectFirst("img")?.attr("data-src") 
            ?: element.selectFirst("img")?.attr("src"))
            
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text()?.trim() ?: "Film"
        val poster = fixUrlNull(document.selectFirst("div.poster img")?.attr("src"))
        val description = document.selectFirst("div.wp-content, div.synopsis")?.text()?.trim()

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val iframes = document.select("iframe").mapNotNull { it.attr("src") }
        
        for (iframe in iframes) {
            val fixedUrl = fixUrl(iframe)
            loadExtractor(fixedUrl, subtitleCallback, callback)
        }
        return true
    }
}