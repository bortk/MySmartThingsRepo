/* groovylint-disable CatchException, DuplicateListLiteral, DuplicateMapLiteral, DuplicateNumberLiteral, DuplicateStringLiteral, GStringExpressionWithinString, GetterMethodCouldBeProperty, ImplicitClosureParameter, ImplicitReturnStatement, LineLength, MethodParameterTypeRequired, MethodReturnTypeRequired, NoDef, ParameterReassignment, PublicMethodsBeforeNonPublicMethods, TernaryCouldBeElvis, UnnecessaryElseStatement, UnnecessaryGetter, VariableTypeRequired */
/**
 *  Copyright 2019 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

// import groovy.json.JsonOutput
import physicalgraph.zigbee.zcl.DataType

/* groovylint-disable-next-line CompileStatic */
metadata {
    definition(name: 'B Zigbee Multi Button for Aqara', namespace: 'bortk', author: 'SmartThings', mcdSync: true, ocfDeviceType: 'x.com.st.d.remotecontroller') {
        capability 'Actuator'
        capability 'Button'
        capability 'Holdable Button'
        capability 'Configuration'
        capability 'Refresh'
        capability 'Sensor'
        capability 'Health Check'
    }
    fingerprint deviceJoinName: 'Aqara D1 Double Button', model: 'lumi.remote.b286acn02',  inClusters: '0000,0003,0019,FFFF,0012', outClusters: '0000,0004,0003,0005,0019,FFFF,0012', manufacturer: 'LUMI', profileId: '0104', endpointId: '01'

    tiles {
        standardTile('button', 'device.button', width: 2, height: 2) {
            state 'default', label: '', icon: 'st.unknown.zwave.remote-controller', backgroundColor: '#ffffff'
            state 'button 1 pushed', label: 'pushed #1', icon: 'st.unknown.zwave.remote-controller', backgroundColor: '#00A0DC'
        }

        standardTile('refresh', 'device.refresh', inactiveLabel: false, decoration: 'flat') {
            state 'default', action:'refresh.refresh', icon:'st.secondary.refresh'
        }
        main(['button'])
        details(['button', 'refresh'])
    }

    preferences {
        input name: 'reloadConfig', type: 'bool', title: 'Reload Config?'
        input name: 'deleteChildren', type: 'bool', title: 'Delete Child Devices?'
        //Live Logging Message Display Config
        input description: 'These settings affect the display of messages in the Live Logging tab of the SmartThings IDE.', type: 'paragraph', element: 'paragraph', title: 'Live Logging'
        input name: 'infoLogging', type: 'bool', title: 'Log info messages?', defaultValue: true
        input name: 'debugLogging', type: 'bool', title: 'Log debug messages?', defaultValue: true
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
    displayDebugLog("parseAttrMessage description = ${description} ")
    def descMap = zigbee.parseDescriptionAsMap(description)
    def map = [:]
    displayDebugLog("parseAttrMessage descMap = ${descMap} ")

    def buttonNumber = 0
    def actionValue = ''

    displayDebugLog("parseAttrMessage descMap.cluster = ${descMap.cluster}")
    displayDebugLog("parseAttrMessage descMap.endpoint = ${descMap.endpoint}")
    displayDebugLog("parseAttrMessage descMap.value = ${descMap.value}")
    displayDebugLog("parseAttrMessage descMap.data = ${descMap.data}")

    if (descMap.cluster == '0012') {
        switch (descMap.endpoint) {
            case '01':
                buttonNumber = 1
                break
            case '02':
                buttonNumber = 2
                break
            case '03':
                // both buttons pressed together. Map to button 3
                buttonNumber = 3
                break
        }

        switch (descMap.value) {
            case '0000':
                actionValue = 'held'
                break
            case '0001':
                actionValue = 'pushed'
                break
            case '0002':
                actionValue = 'double-clicked'
                break
        }
    }
    log.debug "parseAttrMessage buttonNumber = ${buttonNumber}"
    log.debug "parseAttrMessage actionValue = ${actionValue}"

    def descriptionText = getButtonName() + " ${buttonNumber} was ${actionValue}"
    log.debug "${descriptionText}"

    if ( buttonNumber > 0 ) {
        log.debug "parseAttrMessage sendEventToChild ${buttonNumber}"
        sendEventToChild(buttonNumber, createEvent(name: 'button', value: actionValue, data: [buttonNumber: buttonNumber], descriptionText: descriptionText, isStateChange: true))
        map = createEvent(name: 'button', value: 'pushed', data: [buttonNumber: buttonNumber], descriptionText: descriptionText, isStateChange: true)
    }
    return map
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
    log.debug 'configure bindings:' + bindings
    def batteryVoltage = 0x21
    def cmds = zigbee.onOffConfig() +
            zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage, DataType.UINT8, 30, 21600, 0x01) +
            zigbee.enrollResponse() +
            zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage) + bindings
    return cmds

    /*
     logging("Sending init command for Opple Remote...", 100)
        sendZigbeeCommands([
            zigbeeReadAttribute(0x0000, 0x0001)[0],
            zigbeeReadAttribute(0x0000, 0x0005)[0], "delay 187",
            zigbeeWriteAttribute(0xFCC0, 0x0009, 0x20, 0x01, [mfgCode: "0x115F"])[0],
            "delay 3001",
            zigbeeWriteAttribute(0xFCC0, 0x0009, 0x20, 0x01, [mfgCode: "0x115F"])[0],
            "delay 3002",
            zigbeeWriteAttribute(0xFCC0, 0x0009, 0x20, 0x01, [mfgCode: "0x115F"])[0]
        ])
        */
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
    displayInfoLog('Initializing Aqara D1 Double Button')
    log.debug 'initialize'
    def numberOfButtons = 3
    log.debug 'numberOfButtons: ' + numberOfButtons
    sendEvent(name: 'numberOfButtons', value: numberOfButtons, isStateChange: false)
    sendEvent(name: 'checkInterval', value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: 'zigbee', hubHardwareId: device.hub.hardwareID])

    if (reloadConfig) {
        configure()
    }

    if (deleteChildren) {
        log.debug ': Deleting child devices'
        device.updateSetting('deleteChildren', false)
        childDevices.each {
            try {
                log.debug(": deleting  child ${it.deviceNetworkId}")
                deleteChildDevice(it.deviceNetworkId)
                log.debug(": deleted child ${it.deviceNetworkId}")
            }
            catch (e) {
                log.debug "Error deleting ${it.deviceNetworkId}: ${e}"
            }
        }
    }

    if (!childDevices) {
        addChildButtons(numberOfButtons)
    }
    if (childDevices) {
        def event
        for (def endpoint : 1..device.currentValue('numberOfButtons')) {
            event = createEvent(name: 'button', value: 'pushed', isStateChange: true)
            sendEventToChild(endpoint, event)
        }
    }

    sendEvent(name:'pushed', value: 0, isStateChange: false, descriptionText: 'Refresh of pushed state')
    sendEvent(name:'held', value: 0, isStateChange: false, descriptionText: 'Refresh of held state')
    sendEvent(name:'lastHoldEpoch', value: 0, isStateChange: false, descriptionText: 'Refresh of lastHoldEpoch')
    sendEvent(name:'doubleTapped', value: 0, isStateChange: false, descriptionText: 'Refresh of double-tapped state')
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
            log.debug "button: ${endpoint}  created"
            log.debug "child: ${child}  created"
            child.sendEvent(name: 'supportedButtonValues', value: supportedButtonValues.encodeAsJSON(), displayed: false)
            log.debug "supportedButtonValues: ${supportedButtonValues}"
        } catch (Exception e) {
            log.debug "Exception: ${e}"
        }
    }
}

private getSupportedButtonValues() {
    def values
    values = ['pushed', 'held', 'double']
    return values
}

private getModelBindings() {
    log.debug 'getModelBindings()'
    def bindings = []
    for (def endpoint : 1..3) {
        bindings += zigbee.addBinding(zigbee.ONOFF_CLUSTER, ['destEndpoint' : endpoint])
    }
    bindings
}

private getButtonName() {
    def values = device.displayName.endsWith(' 1') ? "${device.displayName[0..-2]}" : "${device.displayName}"
    return values
}

private displayDebugLog(message) {
    if (debugLogging) {
        log.debug "${device.displayName}${message}"
    }
}
private displayInfoLog(message) {
    if (infoLogging) {
        log.info "${device.displayName}${message}"
    }
}
