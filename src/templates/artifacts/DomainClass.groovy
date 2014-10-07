@artifact.package@


class @artifact.name@ implements java.io.Serializable {

    static constraints = {
	}
	
	String uid
	
	def beforeValidate() {
		if (uid == null)
			uid = java.util.UUID.randomUUID().toString();
	}
}
