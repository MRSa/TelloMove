package jp.osdn.gokigen.tellomove.communication

class StatusReceiver(private val ipAddress: String = "192.168.10.1", private val statusPortNo: Int = 8890)
{


    fun startReceive()
    {



    }

    companion object
    {
        private val TAG = CommandPublisher::class.java.simpleName
        private const val BUFFER_SIZE = 1024 * 1024 + 16 // 受信バッファは 1MB
        private const val COMMAND_SEND_RECEIVE_DURATION_MS = 30
        private const val COMMAND_SEND_RECEIVE_DURATION_MAX = 3000
        private const val COMMAND_POLL_QUEUE_MS = 15
    }
}