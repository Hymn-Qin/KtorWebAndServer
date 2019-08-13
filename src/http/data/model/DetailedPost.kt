package http.data.model

data class DetailedPost(
    val id: Int,
    val author: SimpleUser,
    val title: String,
    val content: String,
    val replies: List<Reply>
)