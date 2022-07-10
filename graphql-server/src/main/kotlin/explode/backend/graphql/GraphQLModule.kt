package explode.backend.graphql

import com.expediagroup.graphql.generator.extensions.print
import explode.dataprovider.provider.IBlowDataProvider
import explode.dataprovider.provider.IBlowResourceProvider
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * GraphQL Module for Ktor
 *
 * providing following endpoints:
 * 	- / => welcome message only
 * 	- /graphql =(POST)=> the actual backend part of the Dynamite server
 * 	- /graphql =(GET)=> the playground of GraphQL
 * 	- /sdl =(GET)=> the schema data
 */
fun Application.graphQLModule(blow: IBlowDataProvider, usePlayground: Boolean) {
	routing {
		get {
			call.respondText("You're finally here. Brother Slayer. Spawn Killer.")
		}

		post("graphql") {
			KtorServer(blow).handle(call)
		}

		if(usePlayground) {
			get("sdl") {
				call.respondText(getGraphQLSchema(blow).print())
			}

			get("graphql") {
				call.respondText(buildPlaygroundHtml("graphql", "subscriptions"), ContentType.Text.Html)
			}
		}
	}
}

/**
 * Resource Dispatch Server Module
 *
 * providing following endpoints:
 *  - /download/music/encoded/(set-id)
 *  - /download/cover/encoded/(set-id)
 *  - /download/chart/encoded/(set-id)
 *  - /download/preview/encoded/(set-id)
 *  - /download/avatar/256x256_jpg/(user-id)
 *  - /download/cover/480x270_jpg/(set-id)
 */
fun Application.dynamiteResourceModule(blowResource: IBlowResourceProvider) {
	routing {
		route("download") {
			get("/music/encoded/{setId}") {
				val id = this.call.parameters["setId"]
				val f = blowResource.getMusicResource(id)
				if(f != null) {
					call.respondBytes(f)
				} else {
					call.respond(HttpStatusCode.NotFound, "")
				}
			}
			get("/cover/encoded/{setId}") {
				val id = this.call.parameters["setId"]
				val f = blowResource.getSetCoverResource(id)
				if(f != null) {
					call.respondBytes(f)
				} else {
					call.respond(HttpStatusCode.NotFound, "")
				}
			}
			get("/preview/encoded/{setId}") {
				val id = this.call.parameters["setId"]
				val f = blowResource.getPreviewResource(id)
				if(f != null) {
					call.respondBytes(f)
				} else {
					call.respond(HttpStatusCode.NotFound, "")
				}
			}
			get("/chart/encoded/{chartId}") {
				val id = this.call.parameters["chartId"]
				val f = blowResource.getChartResource(id)
				if(f != null) {
					call.respondBytes(f)
				} else {
					call.respond(HttpStatusCode.NotFound, "")
				}
			}
			get("/avatar/256x256_jpg/{userId}") {
				val uid = call.parameters["userId"]
				val f = blowResource.getUserAvatarResource(uid)
				if(f != null) {
					call.respondBytes(f)
				} else {
					call.respond(HttpStatusCode.NotFound, "")
				}
			}
			get("/cover/480x270_jpg/{setId}") {
				val id = this.call.parameters["chartId"]
				val f = blowResource.getStorePreviewResource(id)
				if(f != null) {
					call.respondBytes(f)
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