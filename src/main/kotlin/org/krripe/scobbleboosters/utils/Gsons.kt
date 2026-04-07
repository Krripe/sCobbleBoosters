package org.krripe.scobbleboosters.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder

object Gsons {
    val default: Gson by lazy {
        GsonBuilder()
            .disableHtmlEscaping()
            .create()
    }

    val pretty: Gson by lazy {
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
    }
}
