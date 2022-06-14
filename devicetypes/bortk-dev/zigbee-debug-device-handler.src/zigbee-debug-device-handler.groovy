// import groovy.json.JsonOutput
import physicalgraph.zigbee.zcl.DataType

/* groovylint-disable-next-line CompileStatic */
metadata {
    definition (name: 'Zigbee Debug Device Handler', namespace: 'bortk-dev', author: 'SmartThings', mcdSync: true, ocfDeviceType: 'x.com.st.d.remotecontroller') {
        capability 'Actuator'
        capability 'Button'
        capability 'Holdable Button'
        capability 'Configuration'
        capability 'Refresh'
        capability 'Sensor'
        capability "Switch"
        capability "Momentary"
        capability "Temperature Measurement"
        capability "Health Check"
        capability "Power Meter"
        capability "Polling"
    }

    tiles {
        standardTile('button', 'device.button', width: 2, height: 2) {
            state 'default', label: '', icon: 'st.unknown.zwave.remote-controller', backgroundColor: '#ffffff'
            state 'button 1 pushed', label: 'pushed #1', icon: 'st.unknown.zwave.remote-controller', backgroundColor: '#00A0DC'
        }

        standardTile('refresh', 'device.refresh', inactiveLabel: false, decoration: 'flat') {
            state 'default', action:'refresh.refresh', icon:'st.secondary.refresh'
        }
        main (['button'])
        details(['button', 'refresh'])
    }

    preferences {
        input name: "infoLogging", type: "bool", title: "Display info log messages?", required: false, displayDuringSetup: false
		input name: "debugLogging", type: "bool", title: "Display debug log messages?", required: false, displayDuringSetup: false
        input name: "numberOfButtons", type: "int", title: "Number Of Buttons", required: true, displayDuringSetup: true 
    }
}

def parse(String description) {

    def counter = now() % 100

    log.debug "****** Parse Description START ***** ${counter}"
    log.debug "${description} "
    def result = parseAttrMessage(description)
    log.debug "result ${result} "
    log.debug "------ Parse Description END ----- ${counter}"
    log.debug ''
    return result
}


def parseAttrMessage(description) {
    def descMap = zigbee.parseDescriptionAsMap(description)
    def map = [:]
    log.debug "parseAttrMessage descMap = ${descMap} "
    // log.debug "parseAttrMessage descMap.clusterInt = ${descMap.clusterInt} "
    // log.debug "parseAttrMessage descMap.sourceEndpoint = ${descMap.sourceEndpoint} "
    // log.debug "parseAttrMessage descMap.commandInt = ${descMap.commandInt} "
    // log.debug "parseAttrMessage descMap.data = ${descMap.data} "
    // log.debug "parseAttrMessage descMap.data[0] = ${descMap.data[0]} "
    // log.debug "parseAttrMessage descMap.data[1] = ${descMap.data[1]} "
    // log.debug "parseAttrMessage descMap.data[2] = ${descMap.data[2]} "
/*
    int code = -1
    // log.debug "parseAttrMessage code = $code "
    if (descMap.data[0] != null) {
        code = (descMap.data[0] as int)
    }

    // log.debug "parseAttrMessage code = $code "
    def buttonNumber
    if (descMap?.clusterInt == 6) {
        // log.debug 'Button group C (6)'
        code = descMap.commandInt as int
        if (code == 0) {
            log.debug 'Button 1'
            buttonNumber = 1
        }
        else if (code == 1) {
            log.debug 'Button 2'
            buttonNumber = 2
        }

    }
    else  if (descMap?.clusterInt ==  8){
        // log.debug 'Button group B (8)'
        if (code == 1) {
            log.debug 'Button 3'
            buttonNumber = 3
        }
        else if (code == 0) {
            log.debug 'Button 4'
            buttonNumber = 4
        }
    }
    else if (descMap?.clusterInt ==  768) {
        // log.debug 'Button group A (768)'
        if (code == 1) {
            log.debug 'Button 5'
            buttonNumber = 5
        }
        else if (code == 3) {
            log.debug 'Button 6'
            buttonNumber = 6
        }
    }

    def descriptionText = getButtonName() + " ${buttonNumber} was pushed"
    sendEventToChild(buttonNumber, createEvent(name: 'button', value: 'pushed', data: [buttonNumber: buttonNumber], descriptionText: descriptionText, isStateChange: true))
    map = createEvent(name: 'button', value: 'pushed', data: [buttonNumber: buttonNumber], descriptionText: descriptionText, isStateChange: true)
    map
    */
}

def sendEventToChild(buttonNumber, event) {
    String childDni = "${device.deviceNetworkId}:$buttonNumber"
    def child = childDevices.find { it.deviceNetworkId == childDni }
    child?.sendEvent(event)
}


def refresh() {
//     log.debug '#'
    log.debug 'refresh()'
//     // log.debug 'read volt:' + zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage)
//     log.debug '##'
//     zigbee.enrollResponse()
//     return zigbee.readAttribute(zigbee.ONOFF_CLUSTER, switchType) // + zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage)
}

def ping() {
    refresh()
}

def configure() {
    log.debug 'Configure'
    def bindings = getModelBindings()
    def cmds = zigbee.onOffConfig() +
            zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage, DataType.UINT8, 30, 21600, 0x01) +
            zigbee.enrollResponse() +
            zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage) + bindings
    return cmds
}

def installed() {
    log.debug 'installed'
    sendEvent(name: 'button', value: 'pushed', isStateChange: true, displayed: false)
    sendEvent(name: 'supportedButtonValues', value: supportedButtonValues.encodeAsJSON(), displayed: false)
    initialize()
}

def updated() {
    runIn(2, 'initialize', [overwrite: true])
}

def initialize() {
    displayDebugLog('initialize')
  	displayDebugLog( 'numberOfButtons: ' + numberOfButtons)
    sendEvent(name: 'numberOfButtons', value: numberOfButtons, isStateChange: false)
    sendEvent(name: 'checkInterval', value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: 'zigbee', hubHardwareId: device.hub.hardwareID])
   // if (!childDevices) {
     //   addChildButtons(numberOfButtons)
    //}
    if (childDevices) {
        def event
        for (def endpoint : 1..device.currentValue('numberOfButtons')) {
            event = createEvent(name: 'button', value: 'pushed', isStateChange: true)
            sendEventToChild(endpoint, event)
        }
    }

    sendEvent(name:"pushed", value: 0, isStateChange: false, descriptionText: "Refresh of pushed state")
    sendEvent(name:"held", value: 0, isStateChange: false, descriptionText: "Refresh of held state")
    sendEvent(name:"lastHoldEpoch", value: 0, isStateChange: false, descriptionText: "Refresh of lastHoldEpoch")
    sendEvent(name:"doubleTapped", value: 0, isStateChange: false, descriptionText: "Refresh of double-tapped state")
}

private addChildButtons(numberOfButtons) {
    for (def endpoint : 1..numberOfButtons) {
        try {
            String childDni = "${device.deviceNetworkId}:$endpoint"
            def componentLabel = getButtonName() + "${endpoint}"

            def child = addChildDevice('smartthings', 'Child Button', childDni, device.getHub().getId(), [
                    completedSetup: true,
                    label         : componentLabel,
                    isComponent   : true,
                    componentName : "button$endpoint",
                    componentLabel: "Button $endpoint"
            ])
            log.debug 'button: $endpoint  created'
            log.debug 'child: $child  created'
            child.sendEvent(name: 'supportedButtonValues', value: supportedButtonValues.encodeAsJSON(), displayed: false)
        } catch (Exception e) {
            log.debug "Exception: ${e}"
        }
    }
}

private getSupportedButtonValues() {
    def values
    values = ['pushed', 'held']
    return values
}


private getModelBindings() {
    def bindings = []
    for (def endpoint : 1..numberOfButtons) {
        bindings += zigbee.addBinding(zigbee.ONOFF_CLUSTER, ['destEndpoint' : endpoint])
    }
    bindings
}

private getButtonName() {
    def values = device.displayName.endsWith(' 1') ? "${device.displayName[0..-2]}" : "${device.displayName}"
    return values
}

private def displayDebugLog(message) 
{
    if (debugLogging)
		log.debug "${device.displayName} ${message}"
}

private def displayInfoLog(message) 
{
    if (infoLogging)
		log.info "${device.displayName} ${message}"
}