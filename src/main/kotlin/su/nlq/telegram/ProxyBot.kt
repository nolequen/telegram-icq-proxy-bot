package su.nlq.telegram

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.json.JsonMapper
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.*

class ProxyBot : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    companion object {
        private val log = LoggerFactory.getLogger("proxy")
    }

    object Configuration {
        val token by lazy { System.getenv("ICQ_BOT_TOKEN") }
        val chat by lazy { System.getenv("ICQ_CHAT_ID") }
    }

    private val httpClient = OkHttpClient()
    private val mapper = JsonMapper()

    override fun handleRequest(event: APIGatewayProxyRequestEvent, context: Context?): APIGatewayProxyResponseEvent {

        log.info("got request: ${event.body}")

        val message = mapper.readValue(event.body, Update::class.java)

        val icqRequest = icqRequest(message.message.text)

        log.info("send request: $icqRequest")

        val response = httpClient.newCall(icqRequest).execute().body

        log.info("got response: ${response?.string()}")

        return APIGatewayProxyResponseEvent().apply {
            statusCode = 200
            headers = mapOf("Content-type" to "text/plain")
            body = "done"
        }
    }

    private fun icqRequest(message: String): Request {
        return Request.Builder()
            .url("https://botapi.icq.net/im/sendIM")
            .post(
                FormBody.Builder()
                    .add("t", Configuration.chat)
                    .add("message", message)
                    .add("r", UUID.randomUUID().toString())
                    .add("aimsid", Configuration.token)
                    .build()
            )
            .build()
    }
}
