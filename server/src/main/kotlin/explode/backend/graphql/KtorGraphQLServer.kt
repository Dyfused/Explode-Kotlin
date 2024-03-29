package explode.backend.graphql

import com.expediagroup.graphql.dataloader.KotlinDataLoaderRegistryFactory
import com.expediagroup.graphql.server.execution.GraphQLRequestHandler
import com.expediagroup.graphql.server.execution.GraphQLServer
import com.fasterxml.jackson.databind.ObjectMapper
import explode.dataprovider.provider.IBlowAccessor
import io.ktor.server.request.*

class KtorGraphQLServer(
	requestParser: KtorGraphQLRequestParser,
	contextFactory: KtorGraphQLContextFactory,
	requestHandler: GraphQLRequestHandler
): GraphQLServer<ApplicationRequest>(requestParser, contextFactory, requestHandler)

fun getGraphQLServer(mapper: ObjectMapper, blow: IBlowAccessor): KtorGraphQLServer {
	val dataLoaderRegistryFactory = KotlinDataLoaderRegistryFactory()
	val requestParser = KtorGraphQLRequestParser(mapper)
	val contextFactory = KtorGraphQLContextFactory()
	val graphQL = getGraphQLObject(blow)
	val requestHandler = GraphQLRequestHandler(graphQL, dataLoaderRegistryFactory)

	return KtorGraphQLServer(requestParser, contextFactory, requestHandler)
}