

<%=packageName ? "package ${packageName}\n\n" : ''%>class ${className}Controller {
    
    def index = { redirect(action:list,params:params) }

    // the delete, save and update actions only accept POST requests
    static allowedMethods = [delete:'POST', save:'POST', update:'POST']

    def list = {
        params.max = Math.min( params.max ? params.max.toInteger() : 10,  100)
        [ ${propertyName}List: ${className}.list( params ), ${propertyName}Total: ${className}.count() ]
    }

    def show = {
        def ${propertyName} = ${className}.get( params.id )

        if(!${propertyName}) {
            flash.message = "${className} not found with id \${params.id}"
            redirect(action:list)
        }
        else { return [ ${propertyName} : ${propertyName} ] }
    }

    def delete = {
        def ${propertyName} = ${className}.get( params.id )
        if(${propertyName}) {
            try {
                ${propertyName}.delete(flush:true)
                flash.message = "${className} \${params.id} deleted"
                redirect(action:list)
            }
            catch(org.springframework.dao.DataIntegrityViolationException e) {
                flash.message = "${className} \${params.id} could not be deleted"
                redirect(action:show,id:params.id)
            }
        }
        else {
            flash.message = "${className} not found with id \${params.id}"
            redirect(action:list)
        }
    }

    def edit = {
        def ${propertyName} = ${className}.get( params.id )

        if(!${propertyName}) {
            flash.message = "${className} not found with id \${params.id}"
            redirect(action:list)
        }
        else {
            return [ ${propertyName} : ${propertyName} ]
        }
    }

    def update = {
        def ${propertyName} = ${className}.get( params.id )
        if(${propertyName}) {
            if(params.version) {
                def version = params.version.toLong()
                if(${propertyName}.version > version) {
                    <%def lowerCaseName = grails.util.GrailsNameUtils.getPropertyName(className)%>
                    ${propertyName}.errors.rejectValue("version", "${lowerCaseName}.optimistic.locking.failure", "Another user has updated this ${className} while you were editing.")
                    render(view:'edit',model:[${propertyName}:${propertyName}])
                    return
                }
            }
            ${propertyName}.properties = params
            if(!${propertyName}.hasErrors() && ${propertyName}.save()) {
                flash.message = "${className} \${params.id} updated"
                redirect(action:show,id:${propertyName}.id)
            }
            else {
                render(view:'edit',model:[${propertyName}:${propertyName}])
            }
        }
        else {
            flash.message = "${className} not found with id \${params.id}"
            redirect(action:list)
        }
    }

    def create = {
        def ${propertyName} = new ${className}()
        ${propertyName}.properties = params
        return ['${propertyName}':${propertyName}]
    }

    def save = {
        def ${propertyName} = new ${className}(params)
        if(!${propertyName}.hasErrors() && ${propertyName}.save()) {
            flash.message = "${className} \${${propertyName}.id} created"
            redirect(action:show,id:${propertyName}.id)
        }
        else {
            render(view:'create',model:[${propertyName}:${propertyName}])
        }
    }

    
    // Base actions for gdsflex
         
    def find = {
    	// Lookup an entity instance by id
    	
        def ${propertyName} = ${className}.get(params.id)

        return [${propertyName}: ${propertyName}]
    }

    def remove = {
		// Remove an entity by id (delete cannot be used from Flex because it's a keyword)    
    
        def ${propertyName} = ${className}.get(params.id)
        if (${propertyName})
            ${propertyName}.delete()
    }

    def merge = {
    	// Merge detached entity from Flex
    	
        def ${propertyName} = params.${propertyName}
        
        if (!${propertyName}.validate())
            throw new org.granite.tide.spring.SpringValidationException(${propertyName}.errors);
        
        ${propertyName} = ${propertyName}.merge()
        
        return [${propertyName}: ${propertyName}]
    }

    def persist = {
    	// Persist a new entity received from Flex
    	
        def ${propertyName} = params.${propertyName}
        
        if (!${propertyName}.validate())
            throw new org.granite.tide.spring.SpringValidationException(${propertyName}.errors);
        
        ${propertyName}.save()
        
        return [${propertyName}: ${propertyName}]
    }
    
    def upload = {
    	// Handle file upload from a Flex FileReference
    	// Supports byte[] or Blob mappings
    	
    	def ${propertyName} = ${className}.get(params.id)
    	
    	if (params[params.property]) {
    		if (java.sql.Blob.class.isAssignableFrom(${className}.metaClass.getMetaProperty(params.property).type)) {
    			${propertyName}[params.property] = org.hibernate.Hibernate.createBlob(params[params.property].getInputStream())
    		}
    		else {
    			def baos = new java.io.ByteArrayOutputStream()
    			baos << params[params.property].getInputStream()
    			${propertyName}[params.property] = baos.toByteArray()
    		}
    	}
    	else
    		${propertyName}[params.property] = null;
    	
    	${propertyName}.save(flush:true)
    	
    	// Init GraniteDS thread context (we are not in a normal AMF request)
    	def graniteConfig = org.granite.config.GraniteConfig.loadConfig(servletContext)
    	def servicesConfig = org.granite.config.flex.ServicesConfig.loadConfig(servletContext)
        def context = org.granite.messaging.webapp.HttpGraniteContext.createThreadIntance(
            graniteConfig, servicesConfig, servletContext,
            request, response
        ) 	
    	// Encode updated entity in AMF to return to Flex 
        def baos = new java.io.ByteArrayOutputStream()
    	def output = graniteConfig.newAMF3Serializer(baos)
    	output.writeObject(${propertyName})
    	output.flush()
    	// Encode in Base64 (Flex file upload responses can only be strings) 
    	render(org.granite.util.Base64.encodeToString(baos.toByteArray(), false))
    }
    
    def download = {
    	// Handle download of a binary property
    	// Supports byte[] and Blob
    	
    	def ${propertyName} = ${className}.get(params.id)
    
    	if (${propertyName}[params.property] instanceof java.sql.Blob)
    		response.outputStream << ${propertyName}[params.property].getBinaryStream()
    	else
    		response.outputStream << ${propertyName}[params.property];
    }
}
