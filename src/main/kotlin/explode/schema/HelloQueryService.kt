@file:Suppress("UNUSED_PARAMETER")

package explode.schema

import com.expediagroup.graphql.server.operations.Query
import java.time.Instant
import java.util.*

object HelloQueryService : Query {

	fun hello(uuid: UUID?) = "${Instant.now()}!"
}