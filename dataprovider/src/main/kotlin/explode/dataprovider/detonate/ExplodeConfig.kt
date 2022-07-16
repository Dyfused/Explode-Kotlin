package explode.dataprovider.detonate

import TConfig.Configuration

/**
 * The configurations that pass to DataProviders.
 */
interface ExplodeConfig {

	/**
	 * The actual instance of the configuration.
	 */
	val config: Configuration

	companion object {
		fun Configuration.explode() = object : ExplodeConfig {
			override val config: Configuration
				get() = this@explode
		}
	}
}