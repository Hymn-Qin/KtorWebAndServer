package http.data.model

data class Reply(
    override val id: Int,
    val postId: Int,
    val content: String
): HasId