package com.geely.gic.hmi.data.model

data class PostSnippet(val snippet: PostSnippet.Text) {
    data class Text(val text: String)
}