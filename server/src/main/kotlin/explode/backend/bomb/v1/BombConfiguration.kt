package explode.backend.bomb.v1

import TConfig.Configuration
import explode.dataprovider.util.ConfigPropertyDelegates.delegateBoolean
import explode.dataprovider.util.ConfigPropertyDelegates.delegateInt

object BombConfiguration : Configuration(BombFolder.resolve("config.cfg")) {

	val useInvitationCode by get(
		"invitation-code",
		"enable",
		true,
		"True if registration requires invitation code."
	).delegateBoolean()

	val costInvitationCode by get(
		"invitation-code",
		"diamond-cost",
		1,
		"The cost of generating new Invitation code."
	).delegateInt()

}