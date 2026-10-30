package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class DiziSolProvider : MainAPI() {
    override var name = "DiziSol"
    override var mainUrl = "https://dizisol.com"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.TvSeries)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/diziler/page/$page/").document
        val series = document.select("div.box-item, article").mapNotNull { toSearchResult(it) }
        return HomePageResponse(listOf(HomePageList("Popüler Diziler", series)))
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("div.box-item, article").mapNotNull { toSearchResult(it) }
    }

    private fun toSearchResult(element: Element): SearchResponse? {
        val title = element.selectFirst(".title, h2")?.text() ?: return null
        val href = fixUrlNull(element.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(element.selectFirst("img")?.attr("src"))

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text()?.trim() ?: "Dizi"
        val poster = fixUrlNull(document.selectFirst(".poster img")?.attr("src"))
        val description = document.selectFirst(".summary, .overview")?.text()?.trim()

        val episodes = mutableListOf<Episode>()
        document.select(".episode-item, li.episodes").forEach { ep ->
            val epHref = fixUrlNull(ep.selectFirst("a")?.attr("href")) ?: return@forEach
            val epName = ep.selectFirst(".ep-title, span")?.text() ?: "Bölüm"
            val seasonNum = ep.attr("data-season").toIntOrNull() ?: 1
            val epNum = ep.attr("data-episode").toIntOrNull() ?: 1

            episodes.add(
                newEpisode(epHref) {
                    this.name = epName
                    this.season = seasonNum
                    this.episode = epNum
                }
            )
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
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
        val iframeSrc = fixUrlNull(document.selectFirst("div.video-content iframe")?.attr("src")) ?: return false
        loadExtractor(iframeSrc, subtitleCallback, callback)
        return true
    }
}