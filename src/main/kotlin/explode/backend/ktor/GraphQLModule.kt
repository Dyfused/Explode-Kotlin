package explode.backend.ktor

import com.expediagroup.graphql.generator.extensions.print
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.graphQLModule() {
	install(Routing)

	routing {
		post("graphql") {
			KtorServer().handle(this.call)
		}

		get("sdl") {
			call.respondText(graphQLSchema.print())
		}

		get("playground") {
			this.call.respondText(buildPlaygroundHtml("graphql", "subscriptions"), ContentType.Text.Html)
		}
	}
}

@Suppress("SameParameterValue")
private fun buildPlaygroundHtml(graphQLEndpoint: String, subscriptionsEndpoint: String) =
	Application::class.java.classLoader.getResource("graphql-playground.html")?.readText()
		?.replace("\${graphQLEndpoint}", graphQLEndpoint)
		?.replace("\${subscriptionsEndpoint}", subscriptionsEndpoint)
		?: error("graphql-playground.html cannot be found in the classpath")