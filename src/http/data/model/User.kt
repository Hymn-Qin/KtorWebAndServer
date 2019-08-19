package http.data.model

data class User(
    override val id: Int,
    val login: String,
    val name: String
): HasId

