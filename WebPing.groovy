/**
 *  WebPing
 *
 *  Copyright 2017 Ed Jakubowski
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
definition(
    name: "WebPing",
    namespace: "WebPing",
    author: "Ed Jakubowski",
    description: "Ping a web server",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    singleInstance: true
    )


preferences {
	page(name: "webDiscovery", title:"Ping a local web server", content:"webDiscovery")
}

def webDiscovery()
{
	log.debug "refreshed webDiscovery called at ${new Date()}"
	//log.debug "device.deviceNetworkId = ${device.deviceNetworkId}"
		int refreshCount = !state.refreshCount ? 0 : state.refreshCount as int
		state.refreshCount = refreshCount + 1
		def refreshInterval = 3

        def options = getServers()
        
		def numFound = options.size() ?: 0

		if(!state.subscribe) {
			subscribe(location, null, locationHandler, [filterEvents:false])
			state.subscribe = true
		}

		//bridge discovery request every
		//if((refreshCount % 2) == 0) {
			sendWebCommand()
		//}

		return dynamicPage(name:"webDiscovery", title:"Web Ping Started!", refreshInterval:refreshInterval, uninstall: true) {
			section("Please wait while we discover your web servers.") {
				input "manualHost", "text", title: "Manual Hostname", required: false, autoCorrect:false
				input "selectedDevice", "enum", required:false, title:"Select server (${numFound} found)", multiple:true, options:options

			}
		}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
    runIn(10, "doDeviceSync" , [overwrite: false]) //setup ip:port syncing every 5 minutes
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
    //runEvery1Minute(timedHandler)
    	// remove location subscription aftwards
	unsubscribe()
	state.subscribe = false

}

def getServers()
{
	state.servers = state.servers ?: [:]
}

def locationHandler(evt) {
//this locationHandler is used to receive the results from the HTTP Get call
	def description = evt.description
	def hub = evt?.hubId
	def parsedEvent = stringToMap(description)
    def ip = convertHexToIP(parsedEvent.ip)
    def port = convertHexToInt(parsedEvent.port)
    def headers = new String(parsedEvent.headers.decodeBase64())
    def body = new String(parsedEvent.body.decodeBase64())
    log.debug "GOT LOCATION EVT: $description"
	log.debug "ip: ${ip}"
    log.debug "port: ${port}"
	log.debug "headers: ${headers}"
	log.debug "body: ${body}"
	
    def servers = getServers()
    servers << [("${ip}:${port}"):("server ${ip}")]
    //log.debug "parsedEvent: $parsedEvent"

}



private sendWebCommand()
{
	def host = getHostAddress()
    def deviceNetworkId = "1234"
    log.debug "sendWebCommand called at ${new Date()} for host ${host}"
    //def action = new physicalgraph.device.HubAction("0b4D4F5F490000000000000000000000040000000400000000000001", physicalgraph.device.Protocol.LAN, "FFFFFFFF:2710")
	//action.options = [type:"LAN_TYPE_UDPCLIENT"]
     //   device.deviceNetworkId = getHostAddress()
//   log.debug "The DNI configured is $device.deviceNetworkId"
	sendHubCommand(new physicalgraph.device.HubAction("""GET / HTTP/1.1\r\nHOST: $host\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}"))
    log.debug "starting hubAction"
}

// gets the address of the Hub
private getCallBackAddress() {

    return device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
}

// gets the address of the device
private getHostAddress() {
    def ip = "192.168.1.195"//getDataValue("ip")
    def port = "80"//getDataValue("port")

    log.debug "Using IP: $ip and port: $port for device"
    return ip + ":" + port
    //return convertHexToIP(ip) + ":" + convertHexToInt(port)
}

private Integer convertHexToInt(hex) {
    return Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    return [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private String hexToString(String txtInHex)
{
	byte [] txtInByte = new byte [txtInHex.length() / 2];
	int j = 0;
	for (int i = 0; i < txtInHex.length(); i += 2)
	{
			txtInByte[j++] = Byte.parseByte(txtInHex.substring(i, i + 2), 16);
	}
	return new String(txtInByte);
}
