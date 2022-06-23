package explode.backend.ktor

import com.expediagroup.graphql.generator.extensions.print
import explode.blow
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.graphQLModule() {
	install(Routing)

	routing {
		get {
			call.respondText("You're finally here. Brother Slayer. Spawn Killer.")
		}

		post("graphql") {
			KtorServer().handle(call)
		}

		get("sdl") {
			call.respondText(graphQLSchema.print())
		}

		get("playground") {
			call.respondText(buildPlaygroundHtml("graphql", "subscriptions"), ContentType.Text.Html)
		}

		route("download") {
			get("/music/encoded/{setId}") {
				val id = this.call.parameters["setId"]
				val f = blow.getMusicFile(id)
				if(f != null) {
					call.respondFile(f)
				} else {
					call.respond(HttpStatusCode.NotFound, "")
				}
			}
			get("/cover/encoded/{setId}") {
				val id = this.call.parameters["setId"]
				val f = blow.getSetCoverFile(id)
				if(f != null) {
					call.respondFile(f)
				} else {
					call.respond(HttpStatusCode.NotFound, "")
				}
			}
			get("/preview/encoded/{setId}") {
				val id = this.call.parameters["setId"]
				val f = blow.getPreviewFile(id)
				if(f != null) {
					call.respondFile(f)
				} else {
					call.respond(HttpStatusCode.NotFound, "")
				}
			}
			get("/chart/encoded/{chartId}") {
				val id = this.call.parameters["chartId"]
				val f = blow.getChartFile(id)
				if(f != null) {
					call.respondFile(f)
				} else {
					call.respond(HttpStatusCode.NotFound, "")
				}
			}
			get("/avatar/256x256_jpg/{userId}") {
				val uid = call.parameters["userId"]
				val f = blow.getUserAvatarFile(uid)
				if(f != null) {
					call.respondFile(f)
				} else {
					call.respond(HttpStatusCode.NotFound, "")
				}
			}
			get("/cover/480x270_jpg/{setId}") {
				val id = this.call.parameters["chartId"]
				val f = blow.getStorePreviewFile(id)
				if(f != null) {
					call.respondFile(f)
				} else {
					call.respond(HttpStatusCode.NotFound, "")
				}
			}
		}
	}
}

@Suppress("SameParameterValue")
private fun buildPlaygroundHtml(graphQLEndpoint: String, subscriptionsEndpoint: String) =
	Application::class.java.classLoader.getResource("graphql-playground.html")?.readText()
		?.replace("\${graphQLEndpoint}", graphQLEndpoint)
		?.replace("\${subscriptionsEndpoint}", subscriptionsEndpoint)
		?: error("graphql-playground.html cannot be found in the classpath")