package com.geely.gic.hmi.chat

import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession
import kotlinx.coroutines.channels.ClosedSendChannelException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class ChatServer {

    //前端页面而言，其实并不关心后端是 Ktor 或者是别的，只需要后端支持的协议是标准的 WebSocket 即可

    /**
     * Atomic counter used to get unique user-names based on the maxiumum users the server had.
     */
    private val usersCounter = AtomicInteger()
    // members 用于管理用户的连接会话，用于向指定用户发送消息。
    // memberNames 用于管理用户在聊天室的昵称，
    // lastMessages 用于向每个用户同步最新的消息。
    // 此处使用 ConcurrentHashMap，是因为 HashMap 并非线程安全，
    // 而 HashTable 效率低下，然而在协程内往往会有非常激烈的线程竞争，
    // 因此在此处选用 ConcurrentHashMap 来解决问题。
    /**
     * A concurrent map associating session IDs to user names.
     */
    private val memberNames = ConcurrentHashMap<String, String>()
    /**
     * Associates a session-id to a set of websockets.
     * Since a browser is able to open several tabs and windows with the same cookies and thus the same session.
     * There might be several opened sockets for the same client.
     */
    private val members = ConcurrentHashMap<String, MutableList<WebSocketSession>>()
    /**
     * A list of the lastest messages sent to the server, so new members can have a bit context of what
     * other people was talking about before joining.
     */
    private val lastMessages = LinkedList<String>()


    /**
     * 用户加入
     * 用户离开
     * Handles that a member identified with a session id and a socket joined.
     */
    suspend fun memberJoin(member: String, socket: WebSocketSession) {
        // Checks if this user is already registered in the server and gives him/her a temporal name if required.
        val name = memberNames.computeIfAbsent(member) { "user${usersCounter.incrementAndGet()}" }

        // Associates this socket to the member id.
        // Since iteration is likely to happen more frequently than adding new items,
        // we use a `CopyOnWriteArrayList`.
        // We could also control how many sockets we would allow per client here before appending it.
        // But since this is a sample we are not doing it.
        val list = members.computeIfAbsent(member) { CopyOnWriteArrayList<WebSocketSession>() }
        list.add(socket)

        // Only when joining the first socket for a member notifies the rest of the users.
        if (list.size == 1) {
            broadcast("server", "Member joined: $name.")
        }
        // Sends the user the latest messages from this server to let the member have a bit context.
        val messages = synchronized(lastMessages) { lastMessages.toList() }
        for (message in messages) {
            socket.send(Frame.Text(message))
        }
    }

    /**
     * 用户离开
     * Handles that a [member] with a specific [socket] left the server.
     */
    suspend fun memberLeft(member: String, socket: WebSocketSession) {
        // Removes the socket connection for this member
        val connections = members[member]
        connections?.remove(socket)

        // If no more sockets are connected for this member, let's remove it from the server
        // and notify the rest of the users about this event.
        if (connections != null && connections.isEmpty()) {
            val name = memberNames.remove(member) ?: member
            broadcast("server", "Member left: $name.")
        }
    }

    /**
     * Handles a [member] idenitified by its session id renaming [to] a specific name.
     */
    private suspend fun memberRenamed(member: String, to: String) {
        // Re-sets the member name.
        val oldName = memberNames.put(member, to) ?: member
        // Notifies everyone in the server about this change.
        broadcast("server", "Member renamed from $oldName to $to")
    }

    /**
     * Handles the 'who' command by sending the member a list of all all members names in the server.
     */
    private suspend fun who(sender: String) {
        members[sender]?.send(Frame.Text(memberNames.values.joinToString(prefix = "[server::who] ")))
    }

    /**
     * Handles the 'help' command by sending the member a list of available commands.
     */
    private suspend fun help(sender: String) {
        members[sender]?.send(Frame.Text("[server::help] Possible commands are: /user, /help and /who"))
    }

    private suspend fun serverBroadcast(message: String) =
        members.values.forEach {
            it.send(Frame.Text("[server] $message"))
        }

    /**
     * Handles sending to a [recipient] from a [sender] a [message].
     *
     * Both [recipient] and [sender] are identified by its session-id.
     */
    private suspend fun sendTo(recipient: String, sender: String, message: String) =
        members[recipient]?.send(Frame.Text("[$sender] $message"))

    /**
     * Handles a [message] sent from a [sender] by notifying the rest of the users.
     */
    suspend fun message(sender: String, message: String) {
        // Pre-format the message to be send, to prevent doing it for all the users or connected sockets.
        val name = memberNames[sender] ?: sender
        val formatted = "[$name] $message"

        // Sends this pre-formatted message to all the members in the server.
        broadcast(formatted)

        // Appends the message to the list of [lastMessages] and caps that collection to 100 items to prevent
        // growing too much.
        synchronized(lastMessages) {
            lastMessages.add(formatted)
            if (lastMessages.size > 100) {
                lastMessages.removeFirst()
            }
        }
    }

    /**
     * Sends a [message] to all the members in the server, including all the connections per member.
     */
    private suspend fun broadcast(message: String) {
        members.values.forEach { socket ->
            socket.send(Frame.Text(message))
        }
    }

    /**
     * Sends a [message] coming from a [sender] to all the members in the server, including all the connections per member.
     */
    private suspend fun broadcast(sender: String, message: String) {
        val name = memberNames[sender] ?: sender
        broadcast("[$name] $message")
    }

    /**
     * Sends a [message] to a list of [this] [WebSocketSession].
     */
    private suspend fun List<WebSocketSession>.send(frame: Frame) =
        forEach {
            try {
                it.send(frame.copy())
            } catch (t: Throwable) {
                try {
                    it.close(Throwable(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "").toString()))
                } catch (ignore: ClosedSendChannelException) {
                    // at some point it will get closed
                }
            }
        }

    /**
     * 消息的收发
     * 对于服务器来说，其实是一个消息的中转站，它接收消息并且转发给相应的用户：
     */
    suspend fun receivedMessage(id: String, command: String) {
        // We are going to handle commands (text starting with '/') and normal messages
        when {
            // The command `who` responds the user about all the member names connected to the user.
            command.startsWith("/who") -> this.who(id)
            // The command `user` allows the user to set its name.
            command.startsWith("/user") -> {
                // We strip the command part to get the rest of the parameters.
                // In this case the only parameter is the user's newName.
                val newName = command.removePrefix("/user").trim()
                // We verify that it is a valid name (in terms of length) to prevent abusing
                when {
                    newName.isEmpty() -> this.sendTo(id, "server::help", "/user [newName]")
                    newName.length > 50 -> this.sendTo(
                        id,
                        "server::help",
                        "new name is too long: 50 characters limit"
                    )
                    else -> this.memberRenamed(id, newName)
                }
            }
            // The command 'help' allows users to get a list of available commands.
            command.startsWith("/help") -> this.help(id)
            // If no commands matched at this point, we notify about it.
            command.startsWith("/") -> this.sendTo(
                id,
                "server::help",
                "Unknown command ${command.takeWhile { !it.isWhitespace() }}"
            )
            // Handle a normal message.
            else -> this.message(id, command)
        }
    }
}