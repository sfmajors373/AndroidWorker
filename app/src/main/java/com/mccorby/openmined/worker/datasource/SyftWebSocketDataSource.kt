package com.mccorby.openmined.worker.datasource

import android.util.Log
import com.mccorby.openmined.worker.datasource.mapper.mapToString
import com.mccorby.openmined.worker.datasource.mapper.mapToSyftMessage
import com.mccorby.openmined.worker.domain.SyftDataSource
import com.mccorby.openmined.worker.domain.SyftMessage
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import io.socket.client.IO
import io.socket.client.Socket

private const val SEND_NEW_MESSAGE = "client_new_message"

private const val TAG = "SyftWebSocketDataSource"

class SyftWebSocketDataSource(private val webSocketUrl: String) : SyftDataSource {
    private lateinit var socket: Socket
    private val publishProcessor: PublishProcessor<SyftMessage> = PublishProcessor.create<SyftMessage>()

    override fun connect() {
        val opts = IO.Options()
        opts.forceNew = true
        socket = IO.socket(webSocketUrl, opts)

        socket.on(Socket.EVENT_MESSAGE) { args -> onEventMessage(args) }

        socket.on(Socket.EVENT_CONNECT) { onConnect() }
        socket.on(Socket.EVENT_DISCONNECT) { onDisconnect() }

        socket.connect()
    }

    override fun disconnect() {
        Log.d(TAG, "Disconnecting")
        socket.disconnect()
    }

    override fun sendMessage(syftMessage: SyftMessage) {
        // Simplify, Serialize, and Compress
        // TODO Add mapper from SyftMessage2ByteArray?.
        Log.d(TAG, "Sending message $syftMessage")
        socket.emit(SEND_NEW_MESSAGE, syftMessage.mapToString())
    }

    override fun onNewMessage(): Flowable<SyftMessage> {
        return publishProcessor.onBackpressureBuffer()
    }

    private fun onConnect() {
        Log.d(TAG, "Connection done")
    }

    private fun onDisconnect() {
        Log.d(TAG, "We're disconnected")
    }

    private fun onEventMessage(vararg args: Any) {
        // Decompress, Deserialise, Build object
        Log.d(TAG, "Received message from the other side")
        val syftMessage = ((args[0] as Array<Any>)[0] as ByteArray).mapToSyftMessage()

        Log.d(TAG, "SyftTensor $syftMessage")

        // TODO Faking message until mapper from incoming message into SyftMessage or SyftTensor is done
        publishProcessor.offer(syftMessage)
    }
}
