package explode.backend.bomb.v1

import explode.dataprovider.model.newUUID
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object InvitationCode {

	private val json = Json { prettyPrint = true }
	private val fileInvitationCode = BombFolder.resolve("invitation.json")
	private var codes: MutableList<InvitationCode> = mutableListOf()

	init {
		codes = json.decodeFromString(fileInvitationCode.readText())
	}

	fun useCode(code: String): Boolean {
		val invitation = codes.find { it.id == code }
		return if(invitation == null) {
			false
		} else {
			codes -= invitation
			updateFile()
			true
		}
	}

	fun generateCode(creatorId: String): String {
		val invitation = InvitationCode(newUUID(), creatorId)
		codes += invitation
		updateFile()
		return invitation.id
	}

	private fun updateFile() {
		fileInvitationCode.writeText(
			json.encodeToString(codes)
		)
	}

	private data class InvitationCode(
		val id: String,
		val creatorId: String
	)

}