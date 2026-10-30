package com.example

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class ExamplePlugin: Plugin() {
    override fun load(context: Context) {
        // Eklediğimiz tüm sağlayıcıları uygulamaya tanıtıyoruz
        registerMainAPI(HdFilmProvider())
        registerMainAPI(DiziSolProvider())
        registerMainAPI(CanliTvProvider())
    }
}