/**
 *  	Denon Network Receiver 
 *    	Based on Denon/Marantz receiver by Kristopher Kubicki
 *    	SmartThings driver to connect your Denon Network Receiver to SmartThings
 *		Tested with AVR-S710W (game1 & game2 inputs are not available), AVR 1912

TESTED DENON MODELS
    ModelId
	EnModelUnknown,		//(0)
	EnModelAVRX10,		//(1)
	EnModelAVRX20,		//(2)
	EnModelAVRX30,		//(3)
	EnModelAVRX40,		//(4)
	EnModelAVRX50,		//(5)
	EnModelAVRX70,		//(6)
	EnModelNR15,		//(7)
	EnModelNR16,		//(8)
	EnModelSR50,		//(9)
	EnModelSR60,		//(10)
	EnModelSR70,		//(11)
	EnModelAV77,		//(12)
	EnModelAV88,		//(13)

ScottE 
Original from https://github.com/sbdobrescu/DenonAVR/blob/master/devicetypes/sb/denon-avr.src/denon-avr.groovy
*/

metadata {
    definition (name: "Denon AVR", namespace: "SB", 
        author: "Bobby Dobrescu") {
        capability "Actuator"
        capability "Switch" 
        capability "Polling"
        capability "Switch Level"
        capability "Music Player" 
        
        attribute "mute", "string"
        attribute "input", "string"     
        attribute "cbl", "string"
        attribute "bd", "string"
		attribute "mp", "string"   
		attribute "zone2", "string" 

        command "mute"
        command "unmute"
        command "toggleMute"
        command "inputSelect", ["string"]
        command "inputNext"
		command "cbl"
		command "bd"
		command "mp"
		command "z2on"        
		command "z2off"        

        }


preferences {
    input("destIp", "text", title: "IP", description: "The device IP")
    input("destPort", "number", title: "Port", description: "The port you wish to connect", defaultValue: 80)
	input(title: "Denon AVR version: ${getVersionTxt()}" ,description: null, type : "paragraph")
}


    simulator {
        // TODO-: define status and reply messages here
    }

    //tiles {
	tiles(scale: 2) {
		multiAttributeTile(name:"multiAVR", type: "mediaPlayer", width: 6, height: 4) {
           tileAttribute("device.status", key: "PRIMARY_CONTROL") { 	            
            	attributeState ("paused", label: 'Paused', backgroundColor: "#53a7c0", defaultState: true)
				attributeState ("playing", label: 'Playing', backgroundColor: "#79b821")
        	}             
            tileAttribute("device.status", key: "MEDIA_STATUS") { 	            
            	attributeState "playing", label: '${name}', action:"switch.off"
                attributeState "paused", label: '${name}', action:"switch.on"
			}  
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
           		attributeState ("level", action:"setLevel")
                }       
            tileAttribute ("device.mute", key: "MEDIA_MUTED") {
            	attributeState("unmuted", action:"mute", nextState: "muted")
            	attributeState("muted", action:"unmute", nextState: "unmuted")
        	}
        }        
        standardTile("input1", "device.cbl", width: 2, height: 2, decoration: "flat"){     
            state "OFF", label: 'TiVo', action: "cbl", icon:"st.Electronics.electronics3", backgroundColor: "#FFFFFF", nextState:"ON"
            state "ON", label: 'TiVo', action: "cbl", icon:"st.Electronics.electronics3" , backgroundColor: "#53a7c0", nextState:"OFF"        
            }
        standardTile("input3", "device.bd", width: 2, height: 2, decoration: "flat"){
        	state "OFF", label: 'Blu-ray', action: "bd", icon:"st.Electronics.electronics8", backgroundColor: "#FFFFFF",nextState:"ON"  
            state "ON", label: 'Blu-ray', action: "bd", icon:"st.Electronics.electronics8", backgroundColor: "#53a7c0", nextState:"OFF"              
        	}
		standardTile("input5", "device.mp", width: 2, height: 2, decoration: "flat"){
        	state "OFF", label: 'Shield', action: "mp", icon:"st.Electronics.electronics5", backgroundColor: "#FFFFFF",nextState:"ON"   
            state "ON", label: 'Shield', action: "mp", icon:"st.Electronics.electronics5", backgroundColor: "#53a7c0", nextState:"OFF"              
			}
	standardTile("zone2", "device.zone2", width: 1, height: 1, inactiveLabel: false, decoration: "flat") {
			state "OFF", label:"Zone 2", action:"z2on", backgroundColor:"#53a7c0", nextState:"on"
			state "ON", label:"Zone 2", action:"z2off", backgroundColor:"#79b821", nextState:"off"
        	}            
		standardTile("poll", "device.poll", width: 1, height: 1, decoration: "flat") {
            state "poll", label: "", action: "polling.poll", icon: "st.secondary.refresh", backgroundColor: "#FFFFFF"
        }
main "multiAVR"
        details(["multiAVR", "input1", "input3", "input5", "zone2", "poll"])
    }
}
def parse(String description) {
	//log.debug "Parsing '${description}'"
 	def map = stringToMap(description)
    if(!map.body || map.body == "DQo=") { return }
	def body = new String(map.body.decodeBase64())
	def statusrsp = new XmlSlurper().parseText(body)
	//POWER STATUS	
    def power = statusrsp.Power.value.text()
	if(power == "ON") { 
    	sendEvent(name: "status", value: 'playing')
    }
    if(power != "" && power != "ON") {  
    	sendEvent(name: "status", value: 'paused')
	}
	//VOLUME STATUS    
    def muteLevel = statusrsp.Mute.value.text()
    if(muteLevel == "on") { 
    	sendEvent(name: "mute", value: 'muted')
	}
    if(muteLevel != "" && muteLevel != "on") {
	    sendEvent(name: "mute", value: 'unmuted')
    }
    if(statusrsp.MasterVolume.value.text()) { 
    	def int volLevel = (int) statusrsp.MasterVolume.value.toFloat() ?: -40.0
        volLevel = (volLevel + 80)
        	log.debug "Adjusted volume is ${volLevel}"
        def int curLevel = 36
        try {
        	curLevel = device.currentValue("level")
        	log.debug "Current volume is ${curLevel}"
        } catch(NumberFormatException nfe) { 
        	curLevel = 36
        }
        if(curLevel != volLevel) {
    		sendEvent(name: "level", value: volLevel)
        }
    } 
	//INPUT STATUS
	def inputCanonical = statusrsp.InputFuncSelect.value.text()
            sendEvent(name: "input", value: inputCanonical)
	        log.debug "Current Input is: ${inputCanonical}"
    
    def inputSurr = statusrsp.selectSurround.value.text()
    		sendEvent(name: "sound", value: inputSurr)
	        log.debug "Current Surround is: ${inputSurr}"  
    def inputZone = statusrsp.RenameZone.value.text()
    		//sendEvent(name: "sound", value: inputSurr)
	        log.debug "Current Active Zone is: ${inputZone}"                      
}
    //TILE ACTIONS
    def setLevel(val) {
        sendEvent(name: "mute", value: "unmuted")     
        sendEvent(name: "level", value: val)
        def int scaledVal = val - 80
        request("cmd0=PutMasterVolumeSet%2F$scaledVal")
    }
    def on() {
        sendEvent(name: "status", value: 'playing')
        request('cmd0=PutZone_OnOff%2FON')
    }
    def off() { 
        sendEvent(name: "status", value: 'paused')
        request('cmd0=PutZone_OnOff%2FOFF')
    }
    def z2on() {
        log.debug "Turning on Zone 2"
        sendEvent(name: "zone2", value: "ON")
        request2('cmd0=PutZone_OnOff%2FON')
        }
    def z2off() {
        log.debug "Turning off Zone 2"
        sendEvent(name: "zone2", value: "OFF")
        request2('cmd0=PutZone_OnOff%2FOFF')
        }
    def mute() { 
        sendEvent(name: "mute", value: "muted")
        request('cmd0=PutVolumeMute%2FON')
    }
    def unmute() { 
        sendEvent(name: "mute", value: "unmuted")
        request('cmd0=PutVolumeMute%2FOFF')
    }
    def toggleMute(){
        if(device.currentValue("mute") == "muted") { unmute() }
        else { mute() }
    }
    def cbl() {
        def cmd = "SAT/CBL"
        log.debug "Setting input to ${cmd}"
        syncTiles(cmd)
        request("cmd0=PutZone_InputFunction%2F" +cmd)
        }
    def bd() {
        def cmd = "BD"
        log.debug "Setting input to ${cmd}"
        syncTiles(cmd)
        request("cmd0=PutZone_InputFunction%2F"+cmd)
        }
    def mp() {
        def cmd = "MPLAY"
        log.debug "Setting input to '${cmd}'"
        syncTiles(cmd)
        request("cmd0=PutZone_InputFunction%2F"+cmd)
        }
    def poll() { 
        //log.debug "Polling requested"
        refresh()
    }
    def syncTiles(cmd){
        if (cmd == "SAT/CBL") sendEvent(name: "cbl", value: "ON")	 
            else sendEvent(name: "cbl", value: "OFF")						
        if (cmd == "BD") sendEvent(name: "bd", value: "ON")	 
            else sendEvent(name: "bd", value: "OFF")						
        if (cmd == "MPLAY") sendEvent(name: "mp", value: "ON")	 
            else sendEvent(name: "mp", value: "OFF")						
    }
    def syncQTiles(cmd){
    }
	def refresh() {
        def hosthex = convertIPtoHex(destIp)
        def porthex = convertPortToHex(destPort)
        device.deviceNetworkId = "$hosthex:$porthex" 

        def hubAction = new physicalgraph.device.HubAction(
                'method': 'GET',
                'path': "/goform/formMainZone_MainZoneXml.xml",
                'headers': [ HOST: "$destIp:$destPort" ] 
            )   
        hubAction
    }
    def request(body) { 
        def hosthex = convertIPtoHex(destIp)
        def porthex = convertPortToHex(destPort)
        device.deviceNetworkId = "$hosthex:$porthex" 

        def hubAction = new physicalgraph.device.HubAction(
                'method': 'POST',
                'path': "/MainZone/index.put.asp",
                'body': body,
                'headers': [ HOST: "$destIp:$destPort" ]
            ) 

        hubAction
    }
    def request2(body) { 
        def hosthex = convertIPtoHex(destIp)
        def porthex = convertPortToHex(destPort)
        device.deviceNetworkId = "$hosthex:$porthex" 

        def hubAction = new physicalgraph.device.HubAction(
                'method': 'POST',
                'path': "/Zone2/index.put.asp",
                'body': body,
                'headers': [ HOST: "$destIp:$destPort" ]
            ) 

        hubAction
    }
    
    
    private String convertIPtoHex(ipAddress) { 
        String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
        return hex
    }
    private String convertPortToHex(port) {
        String hexport = port.toString().format( '%04X', port.toInteger() )
        return hexport
    }
    def getVersionTxt(){
        return "2.1"
    }