package explode

import com.expediagroup.graphql.generator.*
import com.expediagroup.graphql.generator.scalars.IDValueUnboxer
import explode.schema.*
import graphql.GraphQL

private val config = SchemaGeneratorConfig(
	supportedPackages = listOf("explode.schema"),
	hooks = CustomSchemaGeneratorHooks
)
private val queries = listOf(
	TopLevelObject(HelloQueryService),
	TopLevelObject(UserQueryService),
	TopLevelObject(GameDataQueryService),
	TopLevelObject(DownloadQueryService)
)
private val mutations = listOf(
	TopLevelObject(UserMutationService)
)
val graphQLSchema = toSchema(config, queries, mutations)

fun getGraphQLObject(): GraphQL = GraphQL.newGraphQL(graphQLSchema).valueUnboxer(IDValueUnboxer()).build()