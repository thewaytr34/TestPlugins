package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class CanliTvProvider : MainAPI() {
    override var name = "TR Canli TV & Spor"
    override var mainUrl = "https://ardaspor38.top"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Live)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val channels = mutableListOf<SearchResponse>()

        document.select("a.channel-item, a.match-btn, .stream-list a").forEach { elem ->
            val title = elem.text().trim()
            val href = fixUrlNull(elem.attr("href")) ?: return@forEach
            if (title.isNotEmpty()) {
                channels.add(
                    newLiveStreamSearchResponse(title, href, TvType.Live)
                )
            }
        }

        return HomePageResponse(listOf(HomePageList("Canlı Kanallar ve Maçlar", channels)))
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val mainPage = getMainPage(1, MainPageRequest("", ""))
        return mainPage.items.firstOrNull()?.list?.filter {
            it.name.contains(query, ignoreCase = true)
        } ?: emptyList()
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1, .stream-title")?.text()?.trim() ?: "Canlı Yayın"

        return LiveStreamLoadResponse(
            name = title,
            url = url,
            apiName = this.name,
            dataUrl = url
        )
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val html = document.html()
        val m3u8Regex = """(https?://[^\s"'<>]+\.m3u8[^\s"'<>]*)""".toRegex()
        val directMatch = m3u8Regex.find(html)?.value

        if (directMatch != null) {
            callback.invoke(
                ExtractorLink(
                    source = name,
                    name = "$name Canlı HLS",
                    url = directMatch,
                    referer = mainUrl,
                    quality = Qualities.Unknown.value,
                    isM3u8 = true
                )
            )
            return true
        }

        val iframeSrc = fixUrlNull(document.selectFirst("iframe")?.attr("src"))
        if (iframeSrc != null) {
            val iframeDoc = app.get(iframeSrc, referer = mainUrl).document
            val iframeMatch = m3u8Regex.find(iframeDoc.html())?.value
            if (iframeMatch != null) {
                callback.invoke(
                    ExtractorLink(
                        source = name,
                        name = "$name Canlı",
                        url = iframeMatch,
                        referer = iframeSrc,
                        quality = Qualities.Unknown.value,
                        isM3u8 = true
                    )
                )
                return true
            }
        }

        return false
    }
}