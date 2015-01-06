<%=packageName ? "package ${packageName}\n\n" : ''%>class ${className}Controller {

	static allowedMethods = [delete:'POST', save:'POST', update:'POST']

	static defaultAction = 'list'

	def list(Integer max) {
		params.max = Math.min(max ?: 10, 100)
		[${propertyName}List: ${className}.list(params), ${propertyName}Total: ${className}.count() ]
	}

	def show(${className} ${propertyName}) {
		if (!${propertyName}) {
			notFound()
			return
		}
		[${propertyName}: ${propertyName}]
	}

	def delete(${className} ${propertyName}) {
		if (!${propertyName}) {
			notFound()
			return
		}

		try {
			${propertyName}.delete(flush:true)
			flash.message = "${className} \${params.id} deleted"
			redirect(action: 'list')
		}
		catch (org.springframework.dao.DataIntegrityViolationException e) {
			flash.message = "${className} \${params.id} could not be deleted"
			redirect(action: 'show', id: params.id)
		}
	}

	def edit(${className} ${propertyName}) {
		if (!${propertyName}) {
			notFound()
			return
		}

		[${propertyName} : ${propertyName}]
	}

	def update(${className} ${propertyName}) {
		if (!${propertyName}) {
			notFound()
			return
		}

		if (params.version) {
			def version = params.long('version')
			if (${propertyName}.version > version) {
				<%def lowerCaseName = grails.util.GrailsNameUtils.getPropertyName(className)%>
				${propertyName}.errors.rejectValue("version", "${lowerCaseName}.optimistic.locking.failure", "Another user has updated this ${className} while you were editing.")
				render(view: 'edit', model: [${propertyName}: ${propertyName}])
				return
			}
		}

		${propertyName}.properties = params

		if (${propertyName}.hasErrors() || !${propertyName}.save()) {
			render(view: 'edit', model: [${propertyName}: ${propertyName}])
			return
		}

		flash.message = "${className} \${params.id} updated"
		redirect(action: 'show', id: ${propertyName}.id)
	}

	def create() {
		[${propertyName}: new ${className}(params)]
	}

	def save() {
		def ${propertyName} = new ${className}(params)
		if (${propertyName}.hasErrors() || !${propertyName}.save()) {
			render(view: 'create', model: [${propertyName}: ${propertyName}])
			return
		}

		flash.message = "${className} \${${propertyName}.id} created"
		redirect(action: 'show', id: ${propertyName}.id)
	}

	// Base actions for gdsflex

	// Lookup an entity instance by id
	def find(${className} ${propertyName}) {
		[${propertyName}: ${propertyName}]
	}

	// Remove an entity by id (delete cannot be used from Flex because it's a keyword)
	def remove(${className} ${propertyName}) {
		${propertyName}?.delete()
	}

	// Merge detached entity from Flex
	def merge() {

		def ${propertyName} = params.${propertyName}

		if (!${propertyName}.validate()) {
			throw new org.granite.tide.spring.SpringValidationException(${propertyName}.errors)
		}

		${propertyName} = ${propertyName}.merge()

		[${propertyName}: ${propertyName}]
	}

	// Persist a new entity received from Flex
	def persist() {

		def ${propertyName} = params.${propertyName}

		if (!${propertyName}.validate()) {
			throw new org.granite.tide.spring.SpringValidationException(${propertyName}.errors)
		}

		${propertyName}.save()

		[${propertyName}: ${propertyName}]
	}

	// Handle file upload from a Flex FileReference
	// Supports byte[] or Blob mappings
	def upload(${className} ${propertyName}) {

		if (params[params.property]) {
			if (java.sql.Blob.isAssignableFrom(${className}.metaClass.getMetaProperty(params.property).type)) {
				${propertyName}[params.property] = org.hibernate.Hibernate.createBlob(params[params.property].inputStream)
			}
			else {
				def baos = new ByteArrayOutputStream()
				baos << params[params.property].inputStream
				${propertyName}[params.property] = baos.toByteArray()
			}
		}
		else {
			${propertyName}[params.property] = null
		}

		${propertyName}.save(flush:true)

		// Init GraniteDS thread context (we are not in a normal AMF request)
		def graniteConfig = org.granite.config.ServletGraniteConfig.loadConfig(servletContext)
		def servicesConfig = org.granite.config.flex.ServletServicesConfig.loadConfig(servletContext)
		def context = org.granite.messaging.webapp.HttpGraniteContext.createThreadIntance(
			graniteConfig, servicesConfig, servletContext, request, response)
		// Encode updated entity in AMF to return to Flex
		def baos = new ByteArrayOutputStream()
		def output = graniteConfig.newAMF3Serializer(baos)
		output.writeObject(${propertyName})
		output.flush()
		// Encode in Base64 (Flex file upload responses can only be strings)
		render(org.granite.util.Base64.encodeToString(baos.toByteArray(), false))
	}

	// Handle download of a binary property
	// Supports byte[] and Blob
	def download(${className} ${propertyName}) {
		if (${propertyName}[params.property] instanceof java.sql.Blob) {
			response.outputStream << ${propertyName}[params.property].binaryStream
		}
		else {
			response.outputStream << ${propertyName}[params.property]
		}
	}

	private void notFound() {
		flash.message = "${className} not found with id \${params.id}"
		redirect(action: 'list')
	}
}
