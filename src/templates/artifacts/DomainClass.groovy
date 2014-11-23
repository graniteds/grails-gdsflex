@artifact.package@

class @artifact.name@ implements Serializable {

	String uid

	static constraints = {
	}

	def beforeValidate() {
		if (!uid) {
			uid = UUID.randomUUID()
		}
	}
}
