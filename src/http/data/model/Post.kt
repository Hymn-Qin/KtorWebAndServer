package http.data.model

data class Post(
    override val id: Int,
    val authorId: Int,
    val title: String,
    val content: String
): HasId