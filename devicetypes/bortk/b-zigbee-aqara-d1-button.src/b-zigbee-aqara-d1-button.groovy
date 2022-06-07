/* groovylint-disable CatchException, CompileStatic, DuplicateListLiteral, DuplicateMapLiteral, DuplicateNumberLiteral, DuplicateStringLiteral, GStringExpressionWithinString, GetterMethodCouldBeProperty, ImplicitClosureParameter, ImplicitReturnStatement, LineLength, MethodParameterTypeRequired, MethodReturnTypeRequired, NoDef, ParameterReassignment, PublicMethodsBeforeNonPublicMethods, TernaryCouldBeElvis, UnnecessaryElseStatement, UnnecessaryGetter, UnusedImport, VariableTypeRequired */
/**
 *  Copyright 2022 Bortk
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

import groovy.json.JsonOutput
import physicalgraph.zigbee.zcl.DataType

metadata {
    definition(name:'B Zigbee Aqara D1 Button', namespace: 'bortk', author: 'bortk', mcdSync: true, ocfDeviceType: 'x.com.st.d.remotecontroller') {
        capability 'Actuator'
        capability 'Button'
        capability 'Holdable Button'
        capability 'Configuration'
        capability 'Refresh'
        capability 'Sensor'
        capability 'Health Check'

        //fingerprint deviceJoinName: 'Aqara D1 2-button Light Switch (WXKG07LM) - 2020', model: 'lumi.remote.b286acn02',  inClusters: '0000,0003,0019,FFFF,0012', outClusters: '0000,0004,0003,0005,0019,FFFF,0012', manufacturer: 'LUMI', profileId: '0104', endpointId: '01'
    }

    // tiles {
    //     standardTile('button', 'device.button', width: 2, height: 2) {
    //         state 'default', label: '', icon: 'st.unknown.zwave.remote-controller', backgroundColor: '#ffffff'
    //         state 'button 1 pushed', label: 'pushed #1', icon: 'st.unknown.zwave.remote-controller', backgroundColor: '#00A0DC'
    //     }

    //     standardTile('refresh', 'device.refresh', inactiveLabel: false, decoration: 'flat') {
    //         state 'default', action:'refresh.refresh', icon:'st.secondary.refresh'
    //     }
    //     main(['button'])
    //     details(['button', 'refresh'])
    // }

    preferences {
        input name: 'debugLogging', type: 'bool', title: 'Display debug log messages?'
    }
}

def parse(String description) {
    def counter = now() % 100

    log.debug "****** Parse Description START ***** ${counter}"
    // log.debug "${description} "
    def result = parseAttrMessage(description)
    log.debug "result ${result} "
    log.debug "------ Parse Description END ----- ${counter}"
    log.debug ''
    return result
}

def parseAttrMessage(description) {
    log.debug "parseAttrMessage description = ${description} "
    def descMap = zigbee.parseDescriptionAsMap(description)
    def map = [:]
    log.debug "parseAttrMessage descMap = ${descMap} "

    def buttonNumber = 0
    def actionValue = ''

    log.debug "parseAttrMessage descMap.cluster = ${descMap.cluster}"
    log.debug "parseAttrMessage descMap.endpoint = ${descMap.endpoint}"
    log.debug "parseAttrMessage descMap.value = ${descMap.value}"
    log.debug "parseAttrMessage descMap.data = ${descMap.data}"

    if (descMap.cluster == '0012') {
        if (descMap.endpoint == '01') {
            buttonNumber = 1
        }
        else if (descMap.endpoint == '02') {
            buttonNumber = 2
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
    log.debug 'ping'
    refresh()
}

def configure() {
    log.debug 'Configure'

    def bindings = getModelBindings()
    def cmds = zigbee.onOffConfig() + zigbee.enrollResponse() + bindings
    //cmds += zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage, DataType.UINT8, 30, 21600, 0x01)
    // cmds += zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage)
    return cmds
}

def installed() {
    log.debug 'installed'
    sendEvent(name: 'button', value: 'pushed', isStateChange: true, displayed: false)
    sendEvent(name: 'supportedButtonValues', value: supportedButtonValues.encodeAsJSON(), displayed: false)
    initialize()
}

def updated() {
    log.debug 'updated'
    configure()
    runIn(1, 'initialize', [overwrite: true])
}

def initialize() {
    log.debug 'initialize'
    def numberOfButtons = 2
    log.debug 'numberOfButtons: ' + numberOfButtons
    sendEvent(name: 'numberOfButtons', value: numberOfButtons, isStateChange: false)
    sendEvent(name: 'checkInterval', value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: 'zigbee', hubHardwareId: device.hub.hardwareID])
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
    values = ['pushed', 'held', 'single-clicked', 'double-clicked']
    return values
}

private getModelBindings() {
    def bindings = []
    for (def endpoint : 1..2) {
        bindings += zigbee.addBinding(zigbee.ONOFF_CLUSTER, ['destEndpoint' : endpoint])
    }
    bindings
}

private getButtonName() {
    def values = device.displayName.endsWith(' 1') ? "${device.displayName[0..-2]}" : "${device.displayName}"
    return values
}
