import dynamite.SingleQuery
import dynamite.execute
import org.junit.jupiter.api.Test

const val Endpoint = "http://localhost:10443/graphql"
const val AppVersion = ""

class TestDynamite {

	private val loginMutationStr = "mutation login (\$un: String!, \$pw: String!) { r: loginUser (username: \$un, password: \$pw) { _id, username, token, coin, diamond, PPTime, RThisMonth, access { reviewer } } }"

	@Test
	fun testLogin() {
		val loginQuery = SingleQuery(loginMutationStr)
		loginQuery.variables["un"] = "fake-username"
		loginQuery.variables["pw"] = "fake-password"

		loginQuery.execute().let(::println)
	}

	@Test
	fun testRegister() {
		val registerQuery = SingleQuery("mutation reg(\$un:String!, \$pw: String!) { r: registerUser(username: \$un, password: \$pw) { _id, username, token } }")
		registerQuery.variables["un"] = "1"
		registerQuery.variables["pw"] = "1"

		registerQuery.execute().let(::print)
	}

}