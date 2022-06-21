package explode.backend.ktor

import com.expediagroup.graphql.generator.extensions.print
import explode.blow
import explode.blow.provider.mongo.MongoProvider
import explode.blow.provider.mongo.MongoProvider.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.litote.kmongo.eq
import org.litote.kmongo.findOne

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

		// FIXME: 在 IBlowProvider 中添加文件位置获取
		route("download") {
			get("/music/encoded/{setId}") {
				val id = this.call.parameters["setId"]
				val f = (blow as MongoProvider).musicFiles.findOne(IdToFile::_id eq id)
				if(f != null) { call.respondFile(f.file) } else { call.respondText("Invalid setId: $id") }
			}
			get("/cover/encoded/{setId}") {
				val id = this.call.parameters["setId"]
				val f = (blow as MongoProvider).coverFiles.findOne(IdToFile::_id eq id)
				if(f != null) { call.respondFile(f.file) } else { call.respondText("Invalid setId: $id") }
			}
			get("/preview/encoded/{setId}") {
				val id = this.call.parameters["setId"]
				val f = (blow as MongoProvider).previewFiles.findOne(IdToFile::_id eq id)
				if(f != null) { call.respondFile(f.file) } else { call.respondText("Invalid setId: $id") }
			}
			get("/chart/encoded/{chartId}") {
				val id = this.call.parameters["chartId"]
				val f = (blow as MongoProvider).chartFiles.findOne(IdToFile::_id eq id)
				if(f != null) { call.respondFile(f.file) } else { call.respondText("Invalid chartId: $id") }
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