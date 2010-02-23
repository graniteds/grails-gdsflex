flexSDK = System.getenv("FLEX_HOME")
if (buildConfig.flex.sdk) {
	flexSDK = buildConfig.flex.sdk
}
if (!flexSDK) {
	println "No Flex SDK specified. Either set FLEX_HOME in your environment or specify flex.sdk in your grails-app/conf/BuildConfig.groovy file"
	System.exit(1)
}
