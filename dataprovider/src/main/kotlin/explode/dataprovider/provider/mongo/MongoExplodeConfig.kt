package explode.dataprovider.provider.mongo

import TConfig.Configuration
import explode.dataprovider.detonate.ExplodeConfig
import explode.dataprovider.detonate.RCalculators
import explode.dataprovider.util.ConfigPropertyDelegates.delegateBoolean
import explode.dataprovider.util.ConfigPropertyDelegates.delegateInt
import explode.dataprovider.util.ConfigPropertyDelegates.delegateString

class MongoExplodeConfig(override val config: Configuration) : ExplodeConfig {

	val connectionString by config.get(
		"mongo-provider",
		"mongodb-connection-string",
		"mongodb://localhost:27017",
		"The URI of the MongoDB to persistant data."
	).delegateString()

	val databaseName by config.get(
		"mongo-provider",
		"mongodb-database-name",
		"Explode",
		"The database of current Explode."
	).delegateString()

	val allowUnrankedCoin by config.get(
		"mongodb-coin",
		"allow-unranked-coin",
		false, // some charts leveling with insane number can cause coin to overflow.
		"True to enable coin gaining when player finishes a Unranked chart."
	).delegateBoolean()

	val basicCoinValue by config.get(
		"mongodb-coin",
		"basic-coin-factor",
		5,
		"Value for calculating the gaining coin when player finishes a chart.",
		0,
		Int.MAX_VALUE
	).delegateInt()

	val superCoinFactor by config.get(
		"mongodb-coin",
		"super-coin-factor",
		3,
		"Value for calculating the gaining coin when player finishes a chart. The Greater this is, the Smaller result is."
	).delegateInt()

	val maxGainingCoin by config.get(
		"mongodb-coin",
		"max-gaining-coin",
		2000,
		"Value for calculating the gaining coin when player finishes a chart. The maximum number of each coin gaining. 0 to disable."
	).delegateInt()

	val resourceDirectory by config.get(
		"mongodb-resource",
		"resource-directory",
		".explode_data",
		"The path to the directory of data."
	).delegateString()

	val defaultUserAvatar by config.get(
		"mongodb-resource",
		"default-user-avatar",
		"",
		"The ID of the default user avatar. Empty if disable."
	).delegateString()

	val defaultStorePreview by config.get(
		"mongodb-resource",
		"default-store-preview",
		"",
		"The ID of the default set. Empty if disable."
	).delegateString()

	val applyUnencryptedFixes by config.get(
		"unencrypted",
		"use-unencrypted-patches",
		false,
		"True if use the Unencrypted mode, which is unstable and contains conflicts to the Normal mode."
	).delegateBoolean()

	val rScoreAlgorithm by config.get(
		"mongo-r",
		"r-score-algorithm",
		RCalculators.CrazyBull.name,
		"The algorithm to calculate the R score. ${RCalculators.values().map(RCalculators::name)}"
	).delegateString()

	init {
		config.save()
	}

	companion object {
		fun ExplodeConfig.toMongo() = MongoExplodeConfig(config)
	}
}