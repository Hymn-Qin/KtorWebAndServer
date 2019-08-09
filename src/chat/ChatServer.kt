package com.geely.gic.hmi.chat

import chat.data.UserSession
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession
import kotlinx.coroutines.channels.ClosedSendChannelException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class ChatServer {

    //前端页面而言，其实并不关心后端是 Ktor 或者是别的，只需要后端支持的协议是标准的 WebSocket 即可

    //members 用于管理用户的连接会话，用于向指定用户发送消息。
    // memberNames 用于管理用户在聊天室的昵称，
    // lastMessages 用于向每个用户同步最新的消息。
    //此处使用 ConcurrentHashMap，是因为 HashMap 并非线程安全，
    // 而 HashTable 效率低下，然而在协程内往往会有非常激烈的线程竞争，
    // 因此在此处选用 ConcurrentHashMap 来解决问题。
    private val memberNames = ConcurrentHashMap<UserSession, String>()
    private val members = ConcurrentHashMap<UserSession, MutableList<WebSocketSession>>()
    private val lastMessages = LinkedList<String>()


    /**
     * 用户加入
     * 用户离开
     *
     * 其实就是 Session 的加入与离开
     */
    //用户加入
    suspend fun memberJoin(member: UserSession, socket: WebSocketSession) {
        val name = memberNames.computeIfAbsent(member) { member.nickname }
        val list = members.computeIfAbsent(member) { CopyOnWriteArrayList<WebSocketSession>() }
        list.add(socket)
        if (list.size == 1) {
            serverBroadcast("Member joined: $name.")
        }
        val messages = synchronized(lastMessages) { lastMessages.toList() }
        for (message in messages) {
            socket.send(Frame.Text(message))
        }
    }

    //用户离开
    suspend fun memberLeft(member: UserSession, socket: WebSocketSession) {
        val connections = members[member]
        connections?.remove(socket)
        if (connections != null && connections.isEmpty()) {
            val name = memberNames.remove(member) ?: member
            serverBroadcast("Member left: $name.")
        }
    }

    suspend fun memberRenamed(member: UserSession, to: String) {
        val oldName = memberNames.put(member, to) ?: member.nickname
        serverBroadcast("Member renamed from $oldName to $to")
    }


    //chatroomId 的设定，是因为聊天室可能有很多个，
    // 而我们的消息却不可能永远处于全员广播的状态，
    // 有必要按聊天室来拆分具体的请求。此处我们将
    // chatroomId 放在 Session 里，然后可以方便的过滤与消息发送：
    suspend fun broadcast(roomId: String, message: String) =
        members.filter { it.key.chatroomId == roomId }.values.forEach {
            it.send(Frame.Text(message))
        }

    private suspend fun serverBroadcast(message: String) =
        members.values.forEach {
            it.send(Frame.Text("[server] $message"))
        }

    suspend fun sendTo(recipient: UserSession, sender: String, message: String) =
        members[recipient]?.send(Frame.Text("[$sender] $message"))

    suspend fun message(sender: UserSession, message: String) {
        val name = memberNames[sender] ?: sender.nickname
        val formatted = "[$name] $message"
        broadcast(sender.chatroomId, formatted)
        synchronized(lastMessages) {
            lastMessages.add(formatted)
            if (lastMessages.size > 100) {
                lastMessages.removeFirst()
            }
        }
    }

    suspend fun List<WebSocketSession>.send(frame: Frame) =
        forEach {
            try {
                it.send(frame.copy())
            } catch (t: Throwable) {
                try {
                    it.close(Throwable(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "").toString()) )
                } catch (ignore: ClosedSendChannelException) {

                }
            }
        }

    /**
     * 消息的收发
     * 对于服务器来说，其实是一个消息的中转站，它接收消息并且转发给相应的用户：
     */
    suspend fun receivedMessage(id: UserSession, command: String) {
        when {
            command.startsWith("/user") -> {
                val newName = command.removePrefix("/user").trim()
                when {
                    newName.isEmpty() -> sendTo(id, "server::help", "/user [newName]")
                    newName.length > 50 -> sendTo(
                        id,
                        "server::help",
                        "new name is too long: 50 characters limit"
                    )
                    else -> memberRenamed(id, newName)
                }
            }
            else -> message(id, command)
        }
    }


    //client
    //var socket = null;
    //
    //function connect() {
    //    socket = new WebSocket("ws://" + window.location.host + "/ws");
    //    socket.onclose = function(e) {
    //        setTimeout(connect, 5000);
    //    };
    //    socket.onmessage = function(e) {
    //        received(e.data.toString());
    //    };
    //}
    //
    //function received(message) {
    //    // TODO: received message
    //}
}