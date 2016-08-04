/*
 * leakSmart Sensor
 *
 * Version 1.0.7 - Updated to add a compatibility mode for sensors that are not sending 
 *   data as expected, could be related to V1 of the hub.
 * Version 1.0.6 - Decreased frequency of battery reporting from 5 minutes to 4 hours. 
 *	 Increased the wet/dry window from 30 seconds to 1 second. This is just a guess at 
 *   at fix. Decreased frequency of temperature reporting from 30 seconds to 5 minutes. 
 *   (07/28/2016)
 * Version 1.0.5 - Changed the default log level to "INFO" versus "DEBUG" (07/25/2016)
 * Version 1.0.4 - Updated initialziation code which allows the device to pair and 
 *   configure without additional setup, thanks again @krlaframboise (07/24/2016)
 * Version 1.0.3 - Prevented duplicate refresh / configure calls, thanks @krlaframboise
 * Version 1.0.2 - Cleaned up the code by removing un-needed lines of code (07/15/2016)
 * Version 1.0.1 - Added these version numbers (07/15/2016)
 * Version 1.0.0 - Initial Release (07/14/2016)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * This code is by no means 100% all my work. This device handler is the work of SmartThings, @dhelm2, @John_Luikart, & @krlaframboise.
 *
 * You can find this device handler @ https://github.com/ericvitale/ST-leakSmart-Sensor/
 * You can find my other device handlers & SmartApps @ https://github.com/ericvitale
 *
 */

metadata {
    definition (name: "leakSmart Sensor", namespace: "ericvitale", author: "ericvitale@gmail.com", category: "C2") {
        capability "Configuration"
        capability "Battery"
        capability "Refresh"
        capability "Temperature Measurement"
        capability "Water Sensor"
        capability "Polling"
        
		attribute "lastPoll", "number"

        fingerprint profileId: "0104", inClusters: "0000,0001,0003,0020,0402,0B02,FC02", outClusters: "0003,0019"
	}

    simulator {
    }

    preferences {
        section {
            image(name: 'educationalcontent', multiple: true, images: [
                "http://cdn.device-gse.smartthings.com/Moisture/Moisture1.png",
                "http://cdn.device-gse.smartthings.com/Moisture/Moisture2.png",
                "http://cdn.device-gse.smartthings.com/Moisture/Moisture3.png"
            ])
        }
        
        section("Temperature Configuration") {
            input title: "Temperature Offset", description: "This feature allows you to correct any temperature variations by selecting an offset. Ex: If your sensor consistently reports a temp that's 5 degrees too warm, you'd enter '-5'. If 3 degrees too cold, enter '+3'.", displayDuringSetup: false, type: "paragraph", element: "paragraph"
            input "tempOffset", "number", title: "Degrees", description: "Adjust temperature by this many degrees", range: "*..*", displayDuringSetup: false
        }
        
        section("Settings") {
        	input "logging", "enum", title: "Log Level", required: false, defaultValue: "INFO", options: ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"]
            input "v1", "bool", title: "Compatibility Mode?", required: true, defaultValue: false
        }
    }

    tiles(scale: 2) {

        multiAttributeTile(name:"water", type: "generic", width: 6, height: 4){
            tileAttribute ("device.water", key: "PRIMARY_CONTROL") {
                attributeState "dry", label: "Dry", icon:"st.alarm.water.dry", backgroundColor:"#ffffff"
                attributeState "wet", label: "Wet", icon:"st.alarm.water.wet", backgroundColor:"#53a7c0"
            }
        }

        valueTile("temperature", "device.temperature", inactiveLabel: false, width: 2, height: 2) {
            state "temperature", label:'${currentValue}°',
            backgroundColors:[
                [value: 31, color: "#153591"],
                [value: 44, color: "#1e9cbb"],
                [value: 59, color: "#90d2a7"],
                [value: 74, color: "#44b621"],
                [value: 84, color: "#f1d801"],
                [value: 95, color: "#d04e00"],
                [value: 96, color: "#bc2323"]
            ]
        }

        valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
            state "battery", label:'${currentValue}% battery', unit:""
        }

        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        main (["water", "temperature"])
        details(["water", "temperature", "battery", "refresh"])
    }
}

private determineLogLevel(data) {
    switch (data?.toUpperCase()) {
        case "TRACE":
            return 0
            break
        case "DEBUG":
            return 1
            break
        case "INFO":
            return 2
            break
        case "WARN":
            return 3
            break
        case "ERROR":
        	return 4
            break
        default:
            return 1
    }
}

def log(data, type) {
    data = "leakSmart -- ${device.label} -- ${data ?: ''}"
        
    if (determineLogLevel(type) >= determineLogLevel(settings?.logging ?: "INFO")) {
        switch (type?.toUpperCase()) {
            case "TRACE":
                log.trace "${data}"
                break
            case "DEBUG":
                log.debug "${data}"
                break
            case "INFO":
                log.info "${data}"
                break
            case "WARN":
                log.warn "${data}"
                break
            case "ERROR":
                log.error "${data}"
                break
            default:
                log.error "leakSmart -- ${device.label} -- Invalid Log Setting"
        }
    }
}

def updated() {
    log("Update started.", "DEBUG")
	if (!isDuplicateCommand(state.lastUpdated, 5000)) {
        state.lastUpdated = new Date().time

        if (state.configured) {
        	log.debug "in state.configured, next is refresh"
            return response(refresh())
        }
        else {
        	log.debug "!state.configured, next is configure"
            return response(configure())
        }
    }
}

def poll() {
	refresh()
}

def parse(String description) {
    log("parse(${description}.", "DEBUG")

    Map map = [:]
    
    if (description?.startsWith('catchall:')) {
        map = parseCatchAllMessage(description)
    }
    else if (description?.startsWith('read attr -')) {
        map = parseReportAttributeMessage(description)
    }
    else if (description?.startsWith('temperature: ')) {
        map = parseCustomMessage(description)
	}

    log("map = ${map}.", "DEBUG")

	def result = map ? createEvent(map) : null

    return result
}

/* 0000
   0001 - Battery
   0003 - 
   0020
   0402 - Temp
   0B02 - Wet / Dry
   FC02
*/

private Map parseCatchAllMessage(String description) {
    Map resultMap = [:]
    def cluster = zigbee.parse(description)
    if (shouldProcessMessage(cluster)) {
		switch(cluster.clusterId) {
            case 0x0001:
                log("001 Cluster Data: ${cluster.data}.", "DEBUG")
                resultMap = getBatteryResult(cluster.data.last())
                break

            case 0x0402:
               	//temp is last 2 data values. reverse to swap endian
                log("402 Cluster Data: ${cluster.data}.", "DEBUG")
                String temp = cluster.data[-2..-1].reverse().collect { cluster.hex1(it) }.join()
               	def value = getTemperature(temp)
                resultMap = getTemperatureResult(value)
                break
                
            case 0x0B02:
                log("B02 Cluster Data: ${cluster.data}.", "DEBUG")
                String temp = cluster.data[2];
                log.debug "B02 temp data ${temp}"
                resultMap = parseAlarmCode(temp)
                break
                
            default:
            	log("Unhandled Cluster Data: ${cluster.data}.", "WARN")
                break
		}
    } else {
    	log("Did not process message ${cluster}.", "DEBUG")
    }
    
    return resultMap
}

private boolean shouldProcessMessage(cluster) {
    // 0x0B is default response indicating message got through
    // 0x07 is bind message
    boolean ignoredMessage = cluster.profileId != 0x0104 ||
    	cluster.command == 0x0B ||
    	cluster.command == 0x07 ||
    	(cluster.data.size() > 0 && cluster.data.first() == 0x3e)
    
    return !ignoredMessage
}

private Map parseReportAttributeMessage(String description) {
    Map descMap = (description - "read attr - ").split(",").inject([:]) { map, param ->
        def nameAndValue = param.split(":")
        map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
    }
	
    log("Desc Map: $descMap.", "DEBUG")

    Map resultMap = [:]

	log("map = ${map}", "DEBUG")
    
    if (descMap.cluster.toLowerCase() == "0402" && descMap.attrId.toLowerCase() == "0000") {
        def value = getTemperature(descMap.value)
        resultMap = getTemperatureResult(value)
    } else if (descMap.cluster.toLowerCase() == "0001" && descMap.attrId.toLowerCase() == "0020") {
    	resultMap = getBatteryResult(Integer.parseInt(descMap.value, 16))
    } else if (descMap.cluster.toLowerCase() == "0b02" && descMap.attrId.toLowerCase() == "0000") {
        log("Parsing cluster B02 data.", "DEBUG")
    } else if (descMap.cluster.toLowerCase() == "0b02" && descMap.attrId.toLowerCase() == "8101") {
        if(v1) {
        	log("In compatibility mode!", "DEBUG")
            if(descMap.encoding.trim() == "11") {
            	resultMap =  parseAlarmCode("17")
            } else if(descMap.encoding.trim() == "01") {
                resultMap = parseAlarmCode("1")
            }
    	}
    }

    return resultMap
}

private Map parseCustomMessage(String description) {
    Map resultMap = [:]
    
    if (description?.startsWith('temperature: ')) {
    	def value = zigbee.parseHATemperatureValue(description, "temperature: ", getTemperatureScale())
    	resultMap = getTemperatureResult(value)
    }
    
    return resultMap
}

def getTemperature(value) {
    def celsius = Integer.parseInt(value, 16).shortValue() / 100
    
    if(getTemperatureScale() == "C"){
    	return celsius
    } else {
    	return celsiusToFahrenheit(celsius) as Integer
    }
}

private Map getBatteryResult(rawValue) {
    log("Battery rawValue = ${rawValue}.", "DEBUG")
	
    def linkText = getLinkText(device)

    def result = [
        name: 'battery',
    	value: '--',
    	translatable: true
    ]

	def volts = rawValue / 10

    if (rawValue == 0 || rawValue == 255) {
    	 //Nothing
    } else {
        if (volts > 4.8) {
            result.value = 100
            result.descriptionText = "${device.displayName} battery has too much power: (> 3.5) volts."
        } else {
            def minVolts = 2.1
            def maxVolts = 4.5
            def pct = (volts - minVolts) / (maxVolts - minVolts)
            result.value = Math.min(100, (int) pct * 100)
        	result.descriptionText = "${device.displayName} battery was ${result.value}%."
        }
    }

    return result
}

private Map getTemperatureResult(value) {
    log("Begin getTemperatureResult(${value}).", "DEBUG")
    
    if (tempOffset) {
        def offset = tempOffset as int
        def v = value as int
        value = v + offset
    }
    
    def descriptionText
    if ( temperatureScale == 'C' )
    	descriptionText = "${device.displayName} was ${value}°C"
    else
    	descriptionText = "${device.displayName} was ${value}°F"

    return [
        name: 'temperature',
        value: value,
        descriptionText: descriptionText,
        translatable: true
    ]
}

private Map getMoistureResult(value) {
    log("Begin getMoistureResult(${value}).", "DEBUG")
    
    def descriptionText
    
    if ( value == "wet" )
    	descriptionText = "${device.displayName} is wet"
    else
    	descriptionText = "${device.displayName} is dry"
    
    return [
        name: 'water',
        value: value,
        descriptionText: descriptionText,
        translatable: true
    ]
}

private Map parseAlarmCode(value) {

	log("Parse alarm code ${value}.", "DEBUG")

    Map resultMap = [:]

    switch(value) {
        case "1":
            log("Sensor is dry.", "INFO")
            resultMap = getMoistureResult('dry')
            break

        case "17":
            log("Sensor is wet!", "INFO")
            resultMap = getMoistureResult('wet')
            break
	}

    return resultMap
}

def refresh() {
    log("Refreshing...", "INFO")
    
    def retVal = zigbee.readAttribute(0x0402, 0x0000) +
    	zigbee.readAttribute(0x0001, 0x0020)
        
    log("refresh() -- retVal = ${retVal}", "DEBUG")
    
    return retVal
}

/* 0000
   0001 - Battery
   0003 - 
   0020
   0402 - Temp
   0B02 - Wet / Dry
   FC02
*/

def configure() {
    log("Configuring Reporting, IAS CIE, and Bindings.", "DEBUG")
    
    try {
            def retVal = zigbee.configureReporting(0x0001, 0x0020, 0x20, 1440, 21600, 0x01) +
            zigbee.configureReporting(0x0402, 0x0000, 0x29, 300, 3600, 0x0064) +
            zigbee.configureReporting(0x0b02, 0x0000, 0x00, 5, 3600, null) +
            zigbee.readAttribute(0x0402, 0x0000) +
            zigbee.readAttribute(0x0001, 0x0020)

            log("Ending configure(), returning retVal = ${retVal}.", "DEBUG")
            
            state.configured = true
            
			return retVal	
            
    } catch(e) {
            log("ERROR -- ${e}", "ERROR")
    } 
}

private getEndpointId() {
	new BigInteger(device.endpointId, 16).toString()
}

private hex(value) {
	new BigInteger(Math.round(value).toString()).toString(16)
}

private String swapEndianHex(String hex) {
	reverseArray(hex.decodeHex()).encodeHex()
}

private byte[] reverseArray(byte[] array) {
    int i = 0;
    int j = array.length - 1;
    byte tmp;
    while (j > i) {
        tmp = array[j];
        array[j] = array[i];
        array[i] = tmp;
        j--;
        i++;
    }
    return array
}

def isConfigured() {
	if (state.configured == null || state.configured == false) {
    	return false
	} else {
    	return true
    }
}

private isDuplicateCommand(lastExecuted, allowedMil) {
	if(lastExecuted == null) {
    	return true
    } else {
	    !lastExecuted ? false : (lastExecuted + allowedMil > new Date().time) 
    }
}