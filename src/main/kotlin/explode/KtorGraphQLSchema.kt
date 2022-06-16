package explode

import com.expediagroup.graphql.generator.*
import com.expediagroup.graphql.generator.scalars.IDValueUnboxer
import explode.schema.*
import graphql.GraphQL

private val config = SchemaGeneratorConfig(
	supportedPackages = listOf("explode.schema")
)
private val queries = listOf(
	TopLevelObject(HelloQueryService()),
	TopLevelObject(UserQueryService())
)
private val mutations = listOf(
	TopLevelObject(UserLoginMutationService())
)
val graphQLSchema = toSchema(config, queries, mutations)

fun getGraphQLObject(): GraphQL = GraphQL.newGraphQL(graphQLSchema).valueUnboxer(IDValueUnboxer()).build()