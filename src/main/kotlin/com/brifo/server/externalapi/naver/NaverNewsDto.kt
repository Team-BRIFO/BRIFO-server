package com.brifo.server.externalapi.naver

data class NaverNewsSearchResponse(
    val total: Int,
    val start: Int,
    val display: Int,
    val items: List<NaverNewsItem> = emptyList(),
)

data class NaverNewsItem(
    val title: String,
    val originallink: String,
    val link: String,
    val description: String,
    val pubDate: String,
)
