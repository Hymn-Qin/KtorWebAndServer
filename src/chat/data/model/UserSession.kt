package chat.data.model

data class UserSession(val uuid: String,           // UUID
                       var nickname: String,       // 昵称
                       var chatroomId: String      // 聊天房间id
 ) {
}