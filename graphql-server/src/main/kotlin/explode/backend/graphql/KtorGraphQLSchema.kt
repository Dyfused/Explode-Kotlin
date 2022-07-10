package explode.backend.graphql

import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.scalars.IDValueUnboxer
import com.expediagroup.graphql.generator.toSchema
import explode.blow.BlowUtils.mutation
import explode.blow.BlowUtils.query
import explode.dataprovider.provider.IBlowDataProvider
import graphql.GraphQL
import graphql.schema.GraphQLSchema

private val config = SchemaGeneratorConfig(
	supportedPackages = listOf("explode.blow", "explode.dataprovider.model"),
	hooks = CustomSchemaGeneratorHooks
)

private lateinit var schema: GraphQLSchema

fun getGraphQLSchema(blow: IBlowDataProvider): GraphQLSchema {
	if(!::schema.isInitialized) { // lazy init
		schema = toSchema(config, listOf(blow.query), listOf(blow.mutation))
	}

	return schema
}

fun getGraphQLObject(blow: IBlowDataProvider): GraphQL = GraphQL.newGraphQL(getGraphQLSchema(blow)).valueUnboxer(IDValueUnboxer())
	.valueUnboxer(CustomSchemaGeneratorHooks.NNIntUnboxer).build()