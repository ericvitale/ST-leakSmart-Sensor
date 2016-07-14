/*
* Copyright 2016 SmartThings
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
* This code is by no means 100% all my work. This device handler is the work of SmartThings, @dhelm2, & @John_Luikart.
* I've simply adjusted for format, added better loggging, and updated the code to not require the simulator to config. 
* The device configures itself.
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

        fingerprint profileId: "0104", inClusters: "0000,0001,0003,0020,0402,0B02,FC02", outClusters: "0003,0019", manufacturer: "Waxman", model: "leakSmartv2", deviceJoinName: "leakSmart Sensor"
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
        section {
            input title: "Temperature Offset", description: "This feature allows you to correct any temperature variations by selecting an offset. Ex: If your sensor consistently reports a temp that's 5 degrees too warm, you'd enter '-5'. If 3 degrees too cold, enter '+3'.", displayDuringSetup: false, type: "paragraph", element: "paragraph"
            input "tempOffset", "number", title: "Degrees", description: "Adjust temperature by this many degrees", range: "*..*", displayDuringSetup: false
        }
        section {
        	input "logging", "enum", title: "Log Level", required: false, defaultValue: "INFO", options: ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"]
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

def determineLogLevel(data) {
	if(data.toUpperCase() == "TRACE") {
    	return 0
    } else if(data.toUpperCase() == "DEBUG") {
    	return 1
    } else if(data.toUpperCase() == "INFO") {
    	return 2
    } else if(data.toUpperCase() == "WARN") {
    	return 3
    } else {
    	return 4
    }
}

def log(data, type) {
    
    data = "leakSmart -- " + data
    
    try {
        if(determineLogLevel(type) >= determineLogLevel(logging)) {
            if(type.toUpperCase() == "TRACE") {
                log.trace "${data}"
            } else if(type.toUpperCase() == "DEBUG") {
                log.debug "${data}"
            } else if(type.toUpperCase() == "INFO") {
                log.info "${data}"
            } else if(type.toUpperCase() == "WARN") {
                log.warn "${data}"
            } else if(type.toUpperCase() == "ERROR") {
                log.error "${data}"
            } else {
                log.error "leakSmart -- Invalid Log Setting"
            }
        }
    } catch(e) {
    	log.error ${e}
    }
}

def installed() {
    initialize()
}

def updated() {
	initialize()
}

def initialize() {
	configure()
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

    if (description?.startsWith('enroll request')) {
        List cmds = enrollResponse()
        log("cmds = ${cmds}.", "DEBUG")
    	result = cmds?.collect { new physicalgraph.device.HubAction(it) }
    }
    return result
}

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
                log("B02 temp data ${temp}.", "DEBUG")
                return parseAlarmCode(temp)
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
    
    if (descMap.cluster == "0402" && descMap.attrId == "0000") {
        def value = getTemperature(descMap.value)
        resultMap = getTemperatureResult(value)
    } else if (descMap.cluster == "0001" && descMap.attrId == "0020") {
    	resultMap = getBatteryResult(Integer.parseInt(descMap.value, 16))
    } else if (descMap.cluster == "0b02" && descMap.attrId == "0000") {
        log("Parsing cluster B02 data.", "DEBUG")
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
        if (volts > 4.5) {
            result.value = 100
            result.descriptionText = "{{ device.displayName }} battery has too much power: (> 3.5) volts."
        }
        else {
            if (device.getDataValue("manufacturer") == "SmartThings") {
                volts = rawValue // For the batteryMap to work the key needs to be an int
                def batteryMap = [28:100, 27:100, 26:100, 25:90, 24:90, 23:70,
                                  22:70, 21:50, 20:50, 19:30, 18:30, 17:15, 16:1, 15:0]
                def minVolts = 15
                def maxVolts = 28

                if (volts < minVolts)
                    volts = minVolts
                else if (volts > maxVolts)
                    volts = maxVolts
                def pct = batteryMap[volts]
                if (pct != null) {
                    result.value = pct
                    result.descriptionText = "{{ device.displayName }} battery was {{ value }}%"
                }
            }
            else {
                def minVolts = 2.1
                def maxVolts = 4.5
                def pct = (volts - minVolts) / (maxVolts - minVolts)
                result.value = Math.min(100, (int) pct * 100)
                result.descriptionText = "{{ device.displayName }} battery was {{ value }}%"
            }
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
    	descriptionText = '{{ device.displayName }} was {{ value }}°C'
    else
    	descriptionText = '{{ device.displayName }} was {{ value }}°F'

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
    	descriptionText = '{{ device.displayName }} is wet'
    else
    	descriptionText = '{{ device.displayName }} is dry'
    
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
        case "1": // Closed/No Motion/Dry
            log("Sensor is dry.", "DEBUG")
            resultMap = getMoistureResult('dry')
            break

        case "17": // Open/Motion/Wet
            log("Sensor is wet!", "INFO")
            resultMap = getMoistureResult('wet')
            break
	}

    return resultMap
}

def refresh() {
    log.debug "Refreshing"
    def refreshCmds = [
        zigbee.readAttribute(0x0402, 0x0000), "delay 200",
        zigbee.readAttribute(0x0001, 0x0020), "delay 200",
        zigbee.readAttribute(0x0b02, 0x0000), "delay 200"
    ]
    
    log.debug "refreshCmds = ${refreshCmds}."

    return refreshCmds
}

def configure() {
	try {
    sendEvent(name: "checkInterval", value: 7200, displayed: true)
    } catch(e) {
    	log.debug "configure() -- sendEvent() -- ${e}"
    }

	try {
    String zigbeeEui = swapEndianHex(device.hub.zigbeeEui)
    } catch(e) {
    	log.debug "configure() -- swapEndianHex() -- ${e}"
    }
    
    log.debug "Configuring Reporting, IAS CIE, and Bindings."
    
    try{
    def configCmds = [
        zigbee.configureReporting(0x0001, 0x0020, 0x20, 30, 21600, 0x01), "delay 500",
        zigbee.configureReporting(0x0402, 0x0000, 0x29, 30, 3600, 0x0064), "delay 500",
        zigbee.configureReporting(0x0b02, 0x0000, 0x10, 0, 3600, null), "delay 500"
	]
    } catch(e) {
    	log.debug "configure() -- configCmds -- ${e}"
    }

	//return configCmds + refresh() // send refresh cmds as part of config
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