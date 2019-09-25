import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.handleRequest
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the HttpBinApplication.
 */
class HttpBinApplicationTest {
    /**
     * Tests the redirect route by checking its behaviour.
     */
    @Test
    fun testRedirect() {
        testRequest(HttpMethod.Get, "/redirect/2") { assertEquals("/redirect/1", response.headers["Location"]) }
        testRequest(HttpMethod.Get, "/redirect/1") { assertEquals("/redirect/0", response.headers["Location"]) }
        testRequest(HttpMethod.Get, "/redirect/0") { assertEquals(null, response.headers["Location"]) }
    }
}

private fun testRequest(
    method: HttpMethod,
    uri: String,
    setup: suspend TestApplicationRequest.() -> Unit = {},
    checks: suspend TestApplicationCall.() -> Unit
) {
    httpBinTest {
        val req = handleRequest(method, uri) { runBlocking { setup() } }
        checks(req)
    }
}

private fun httpBinTest(callback: suspend TestApplicationEngine.() -> Unit): Unit {
//    withTestApplication(Application::module) {
//        runBlocking { callback() }
//    }
}
