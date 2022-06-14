/* groovylint-disable CatchException, DuplicateListLiteral, DuplicateMapLiteral, DuplicateNumberLiteral, DuplicateStringLiteral, GStringExpressionWithinString, GetterMethodCouldBeProperty, ImplicitClosureParameter, ImplicitReturnStatement, LineLength, MethodParameterTypeRequired, MethodReturnTypeRequired, NoDef, ParameterReassignment, PublicMethodsBeforeNonPublicMethods, TernaryCouldBeElvis, UnnecessaryElseStatement, UnnecessaryGetter, VariableTypeRequired */
/**
 *  Copyright 2022 Boris Tsirulnik
 *
 *  Aqara D1 2-button Light Switch (WXKG07LM) - 2020
 *  Device Handler for SmartThings
 *  Version 0.9.5
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *

 *  Model: Aqara D1 2-button Light Switch (WXKG07LM) - 2020
 *
 *  Supported Actions: Push, Hold, Double-Click
 *
 *  Application Buttons
 *  Main - any button
 *  Button1 - Left button
 *  Button2 - Right button
 *  Button3 - Both buttons clicked together

 *  Baterry reading is not supported
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

        fingerprint deviceJoinName: 'Aqara D1 Double Button', model: 'lumi.remote.b286acn02',  inClusters: '0000,0003,0019,FFFF,0012', outClusters: '0000,0004,0003,0005,0019,FFFF,0012', manufacturer: 'LUMI', profileId: '0104', endpointId: '01'
    }

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
        //section('Buttons Description') {
            input description: 'Supported events for all buttons: Single Click, Double Click and Hold', type: 'paragraph', element: 'paragraph', title: 'Buttons Description'
            input description: 'Any of the 2 buttons was pushed.', type: 'paragraph', element: 'paragraph', title: 'Main Button'
            input description: 'Left Button', type: 'paragraph', element: 'paragraph', title: 'Button1'
            input description: 'Right Button', type: 'paragraph', element: 'paragraph', title: 'Button2'
            input description: 'Both Buttons pushed together', type: 'paragraph', element: 'paragraph', title: 'Button3'
        //}
        //input name: 'reloadConfig', type: 'bool', title: 'Reload Config?'
        //input name: 'deleteChildren', type: 'bool', title: 'Delete Child Devices?'
        //Live Logging Message Display Config
        section('LIVE LOGGING') {
            input description: 'These settings affect the display of messages in the Live Logging tab of the SmartThings IDE.', type: 'paragraph', element: 'paragraph', title: 'Live Logging'
            input name: 'infoLog', type: 'bool', title: 'Log info messages?', defaultValue: true
            input name: 'debugLog', type: 'bool', title: 'Log debug messages?', defaultValue: true
        }
    }
}

def parse(String description) {
    def counter = now() % 100

    debugLog("****** Parse Description START ***** ${counter}")
    debugLog("${description} ")
    def result = parseAttrMessage(description)
    debugLog("result ${result} ")
    debugLog("------ Parse Description END ----- ${counter}")
    debugLog(' ')
    return result
}

def parseAttrMessage(description) {
    debugLog("parseAttrMessage description = ${description} ")
    def descMap = zigbee.parseDescriptionAsMap(description)
    def map = [:]
    debugLog("parseAttrMessage descMap = ${descMap} ")

    def buttonNumber = 0
    def actionValue = ''

    debugLog("parseAttrMessage descMap.cluster = ${descMap.cluster}")
    debugLog("parseAttrMessage descMap.endpoint = ${descMap.endpoint}")
    debugLog("parseAttrMessage descMap.value = ${descMap.value}")
    debugLog("parseAttrMessage descMap.data = ${descMap.data}")

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
                actionValue = 'double'
                break
        }
    }
    debugLog("parseAttrMessage buttonNumber = ${buttonNumber}")
    debugLog("parseAttrMessage actionValue = ${actionValue}")

    debugLog('generating description text')
    def descriptionText = "${getButtonName()} ${buttonNumber} was ${actionValue}"
    debugLog(descriptionText)

    if ( buttonNumber > 0 ) {
        debugLog("parseAttrMessage sendEventToChild ${buttonNumber}")
        sendEventToChild(buttonNumber, createEvent(name: 'button', value: actionValue, data: [buttonNumber: buttonNumber], descriptionText: descriptionText, isStateChange: true))
        map = createEvent(name: 'button', value: actionValue, data: [buttonNumber: buttonNumber], descriptionText: descriptionText, isStateChange: true)
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
    debugLog('refresh()')
//     // log.debug 'read volt:' + zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage)
//     log.debug '##'
//     zigbee.enrollResponse()
//     return zigbee.readAttribute(zigbee.ONOFF_CLUSTER, switchType) // + zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage)
}

def ping() {
    debugLog('ping()')
    refresh()
}

def configure() {
    debugLog('configure()')
    def bindings = getModelBindings()

    def batteryVoltage = 0x21
    def cmds = zigbee.onOffConfig() +
            zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage, DataType.UINT8, 30, 21600, 0x01) +
            zigbee.enrollResponse() +
            zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage) +
            bindings
    return cmds
}

def installed() {
    debugLog('installed()')
    initialize()
}

def updated() {
    debugLog('updated()')
    initialize()
}

def initialize() {
    infoLog('Initializing Aqara D1 Double Button')
    debugLog('initialize()')
    def numberOfButtons = 3
    debugLog('numberOfButtons: ' + numberOfButtons)

    // if (reloadConfig) {
    //     configure()
    // }

    sendEvent(name: 'button', value: 'pushed', isStateChange: true, displayed: false)
    sendEvent(name: 'supportedButtonValues', value: supportedButtonValues.encodeAsJSON(), displayed: false)
    sendEvent(name: 'numberOfButtons', value: numberOfButtons, isStateChange: false)
    sendEvent(name: 'checkInterval', value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: 'zigbee', hubHardwareId: device.hub.hardwareID])

    // if (deleteChildren) {
    //     debugLog( ': Deleting child devices' )
    //     //device.updateSetting('deleteChildren', false)
    //     childDevices.each {
    //         try {
    //             debugLog(": deleting  child ${it.deviceNetworkId}")
    //             deleteChildDevice(it.deviceNetworkId)
    //             debugLog(": deleted child ${it.deviceNetworkId}")
    //         }
    //         catch (e) {
    //             log.debug "Error deleting ${it.deviceNetworkId}: ${e}"
    //         }
    //     }

    //     debugLog(': Deleted child devices')
    // }

    if (!childDevices) {
        debugLog('No child devices. Creating new child devices')
        addChildButtons(numberOfButtons)
    }
    if (childDevices) {
        debugLog('configuring child devices')
        def event
        for (def endpoint : 1..numberOfButtons) {
            event = createEvent(name: 'button', value: 'pushed', isStateChange: true)
            sendEventToChild(endpoint, event)
            debugLog(event)
        // event = createEvent(name: 'supportedButtonValues', value: supportedButtonValues.encodeAsJSON(), displayed: false)
        // sendEventToChild(endpoint, event)
        // debugLog(event)
        }

        // def btnValues = supportedButtonValues.encodeAsJSON()

    // childDevices.each
    // {
    //     // it.setLabel( "")
    //     debugLog(" child device: ${it}")
    //     debugLog(" current: ${it.currentValue('supportedButtonValues')}")
    //     debugLog(" target: ${btnValues}")
    //     if (it.currentValue('supportedButtonValues') != btnValues ) {
    //        // log.warn 'updating supported button values'
    //         event = createEvent(
    //         it.sendEvent(name:'supportedButtonValues', value: btnValues, displayed:false)
    //     }
    // }
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
                    componentName : "button${endpoint}",
                    componentLabel: "ButtonB ${endpoint}"
            ])
            debugLog("button: ${endpoint}  created")
            debugLog("child: ${child}  created")
            child.sendEvent(name: 'supportedButtonValues', value: supportedButtonValues.encodeAsJSON(), displayed: false)
            debugLog("supportedButtonValues: ${supportedButtonValues}")
        } catch (Exception e) {
            log.debug "Exception: ${e}"
        }
    }
// def childL = addChildDevice('smartthings', 'Child Button', "${device.deviceNetworkId}:Left", device.getHub().getId(), [
//                 completedSetup: true,
//                 label         : 'Left Label',
//                 isComponent   : true,
//                 componentName : 'leftcomponentname',
//                 componentLabel: 'Left Component Label'
//         ])
// debugLog('button: Left created')
// childL.sendEvent(name: 'supportedButtonValues', value: supportedButtonValues.encodeAsJSON(), displayed: false)
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

private debugLog(message) {
    if (debugLog) {
        log.debug "${device.displayName}: ${message}"
    }
}
private infoLog(message) {
    if (infoLog) {
        log.info "${device.displayName}: ${message}"
    }
}
