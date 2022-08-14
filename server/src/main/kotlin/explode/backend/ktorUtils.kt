package explode.backend

import explode.backend.bomb.BadResult
import explode.backend.bomb.respondJson
import explode.dataprovider.model.database.MongoUser
import explode.globalJson
import explode.utils.TypedResult
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.decodeFromString

suspend inline fun <reified T> PipelineContext<Unit, ApplicationCall>.payload() =
	globalJson.decodeFromString<T>(call.receiveText())

suspend fun PipelineContext<Unit, ApplicationCall>.checkAuthentication(
	userProvider: (token: String) -> MongoUser?
): TypedResult<MongoUser, Any?> =
	when(val p = call.authentication.principal) {
		null -> {
			call.respondJson(BadResult("Unauthorized"), HttpStatusCode.Unauthorized)
			TypedResult.failure(null)
		}

		is UserIdPrincipal -> {
			val u = userProvider(p.name)
			if(u != null) {
				TypedResult.success(u)
			} else {
				call.respondJson(BadResult("Invalid Token"), HttpStatusCode.Unauthorized)
				TypedResult.failure(null)
			}
		}

		else -> {
			call.respondJson(BadResult("Unreachable"), HttpStatusCode.Unauthorized)
			TypedResult.failure(null)
		}
	}