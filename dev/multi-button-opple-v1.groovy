/* groovylint-disable CatchException, DuplicateListLiteral, DuplicateMapLiteral, DuplicateNumberLiteral, DuplicateStringLiteral, GStringExpressionWithinString, GetterMethodCouldBeProperty, ImplicitClosureParameter, ImplicitReturnStatement, LineLength, MethodParameterTypeRequired, MethodReturnTypeRequired, NoDef, ParameterReassignment, PublicMethodsBeforeNonPublicMethods, TernaryCouldBeElvis, UnnecessaryElseStatement, UnnecessaryGetter, VariableTypeRequired */
/**
 *  Copyright 2019 SmartThings
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
 *    Author: SRPOL
 *    Date: 2019-02-18
 */

// import groovy.json.JsonOutput
import physicalgraph.zigbee.zcl.DataType

/* groovylint-disable-next-line CompileStatic */
metadata {
    definition (name: 'B Zigbee Multi Button for Opple', namespace: 'bortk', author: 'SmartThings', mcdSync: true, ocfDeviceType: 'x.com.st.d.remotecontroller') {
        capability 'Actuator'
        capability 'Battery'
        capability 'Button'
        capability 'Holdable Button'
        capability 'Configuration'
        capability 'Refresh'
        capability 'Sensor'
        capability 'Health Check'

        fingerprint deviceJoinName: 'Aqara Opple 6 Button Remote (WXCJKG13LM)',      model: 'lumi.remote.b686opcn01',     profileId:'0104', inClusters:'0012,0003', outClusters:'0006', manufacturer:'LUMI', application: '11', endpointId: '01'

        fingerprint inClusters: '0000, 0001, 0003, 0007, 0020, 0B05', outClusters: '0003, 0006, 0019', manufacturer: 'CentraLite', model:'3450-L', deviceJoinName: 'Iris Remote Control', mnmn: 'SmartThings', vid: 'generic-4-button' //Iris KeyFob
        fingerprint profileId: '0104', inClusters: '0000,0006,0008,0300', outClusters: '0000', manufacturer: 'LUMI', model: 'lumi.remote.b686opcn01', deviceJoinName: 'Opple Button x6'
        fingerprint inClusters: '0000, 0001, 0003, 0007, 0020, 0B05', outClusters: '0003, 0006, 0019', manufacturer: 'CentraLite', model:'3450-L', deviceJoinName: 'Iris Remote Control', mnmn: 'SmartThings', vid: 'generic-4-button' //Iris KeyFob
        fingerprint inClusters: '0000, 0001, 0003, 0007, 0020, 0B05', outClusters: '0003, 0006, 0019', manufacturer: 'CentraLite', model:'3450-L2', deviceJoinName: 'Iris Remote Control', mnmn: 'SmartThings', vid: 'generic-4-button' //Iris KeyFob
        fingerprint profileId: '0104', inClusters: '0004', outClusters: '0000, 0001, 0003, 0004, 0005, 0B05', manufacturer: 'HEIMAN', model: 'SceneSwitch-EM-3.0', deviceJoinName: 'HEIMAN Remote Control', vid: 'generic-4-button' //HEIMAN Scene Keypad
    }

    tiles {
        standardTile('button', 'device.button', width: 2, height: 2) {
            state 'default', label: '', icon: 'st.unknown.zwave.remote-controller', backgroundColor: '#ffffff'
            state 'button 1 pushed', label: 'pushed #1', icon: 'st.unknown.zwave.remote-controller', backgroundColor: '#00A0DC'
        }

        valueTile('battery', 'device.battery', decoration: 'flat', inactiveLabel: false) {
            state 'battery', label:'${currentValue}% battery', unit:''
        }

        standardTile('refresh', 'device.refresh', inactiveLabel: false, decoration: 'flat') {
            state 'default', action:'refresh.refresh', icon:'st.secondary.refresh'
        }
        main (['button'])
        details(['button', 'battery', 'refresh'])
    }
}

def parse(String description) {

    def counter = now() % 100
   
    log.debug "****** Parse Description START ***** ${counter}"
    log.debug "${description} "
    def result = parseAttrMessage(description)
    log.debug "result ${result} "
    log.debug "------ Parse Description END ----- ${counter}"
    log.debug ""
    return result
}


def parseAttrMessage(description) {
    def descMap = zigbee.parseDescriptionAsMap(description)
    def map = [:]
    log.debug "parseAttrMessage descMap = ${descMap} "
    // log.debug "parseAttrMessage descMap.clusterInt = ${descMap.clusterInt} "
    // log.debug "parseAttrMessage descMap.sourceEndpoint = ${descMap.sourceEndpoint} "
    // log.debug "parseAttrMessage descMap.commandInt = ${descMap.commandInt} "
    /* groovylint-disable-next-line UnnecessaryObjectReferences */
    // log.debug "parseAttrMessage descMap.data = ${descMap.data} "
    // log.debug "parseAttrMessage descMap.data[0] = ${descMap.data[0]} "
    // log.debug "parseAttrMessage descMap.data[1] = ${descMap.data[1]} "
    // log.debug "parseAttrMessage descMap.data[2] = ${descMap.data[2]} "

    int code = -1
    // log.debug "parseAttrMessage code = $code "
    if (descMap.data[0] != null) {
        code = (descMap.data[0] as int)
    }

    // log.debug "parseAttrMessage code = $code "
    def buttonNumber
    if (descMap?.clusterInt == 6){
        // log.debug 'Button group C (6)'
        code = descMap.commandInt as int
        if (code == 0) {
            log.debug 'Button 5'
            buttonNumber = 5
        }
        else if (code == 1){
            log.debug 'Button 6'
            buttonNumber = 6
        }

    }
    else  if (descMap?.clusterInt ==  8){
        // log.debug 'Button group B (8)'
        if (code == 1) {
            log.debug 'Button 3'
            buttonNumber = 3
        }
        else if (code == 0){
            log.debug 'Button 4'
            buttonNumber = 4
        }
    }
    else if (descMap?.clusterInt ==  768){
        // log.debug 'Button group A (768)'
        if (code == 1) {
            log.debug 'Button 1'
            buttonNumber = 1
        }
        else if (code == 3){
            log.debug 'Button 2'
            buttonNumber = 2
        }
    }

    def descriptionText = getButtonName() + " ${buttonNumber} was pushed"
    sendEventToChild(buttonNumber, createEvent(name: 'button', value: 'pushed', data: [buttonNumber: buttonNumber], descriptionText: descriptionText, isStateChange: true))
    map = createEvent(name: 'button', value: 'pushed', data: [buttonNumber: buttonNumber], descriptionText: descriptionText, isStateChange: true)
 
    // if (descMap?.clusterInt == zigbee.ONOFF_CLUSTER && descMap.isClusterSpecific) {
    //     map = getButtonEvent(descMap)
    // } else if (descMap?.clusterInt == 0x0005) {
    //     def buttonNumber
    //     buttonNumber = buttonMap[device.getDataValue('model')][descMap.data[2]]
    //     log.debug "Number is ${buttonNumber}"
    //     def descriptionText = getButtonName() + " ${buttonNumber} was pushed"
    //     sendEventToChild(buttonNumber, createEvent(name: 'button', value: 'pushed', data: [buttonNumber: buttonNumber], descriptionText: descriptionText, isStateChange: true))
    //     map = createEvent(name: 'button', value: 'pushed', data: [buttonNumber: buttonNumber], descriptionText: descriptionText, isStateChange: true)
    // }
    map
}

def getButtonEvent(descMap) {
    if (descMap.commandInt == 1) {
        getButtonResult('press')
    }
    else if (descMap.commandInt == 0) {
        def button = buttonMap[device.getDataValue('model')][descMap.sourceEndpoint]
        getButtonResult('release', button)
    }
}
def getButtonResult2(buttonState, buttonNumber) {
    def event = [:]
    if (buttonState == 'release') {
        def timeDiff = now() - state.pressTime
        if (timeDiff > 10000) {
            return event
        } else {
            buttonState = timeDiff < holdTime ? 'pushed' : 'held'
            def descriptionText = getButtonName() + " ${buttonNumber} was ${buttonState}"
            event = createEvent(name: 'button', value: buttonState, data: [buttonNumber: buttonNumber], descriptionText: descriptionText, isStateChange: true)
            sendEventToChild(buttonNumber, event)
            return createEvent(descriptionText: descriptionText)
        }
    } else if (buttonState == 'press') {
        state.pressTime = now()
        return event
    }
}
def getButtonResult(buttonState, buttonNumber = 1) {
    def event = [:]
    if (buttonState == 'release') {
        def timeDiff = now() - state.pressTime
        if (timeDiff > 10000) {
            return event
        } else {
            buttonState = timeDiff < holdTime ? 'pushed' : 'held'
            def descriptionText = getButtonName() + " ${buttonNumber} was ${buttonState}"
            event = createEvent(name: 'button', value: buttonState, data: [buttonNumber: buttonNumber], descriptionText: descriptionText, isStateChange: true)
            sendEventToChild(buttonNumber, event)
            return createEvent(descriptionText: descriptionText)
        }
    } else if (buttonState == 'press') {
        state.pressTime = now()
        return event
    }
}

def sendEventToChild(buttonNumber, event) {
    String childDni = "${device.deviceNetworkId}:$buttonNumber"
    def child = childDevices.find { it.deviceNetworkId == childDni }
    child?.sendEvent(event)
}


def refresh() {
    return zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage) +
            zigbee.readAttribute(zigbee.ONOFF_CLUSTER, switchType)
// zigbee.enrollResponse()
}

def ping() {
    refresh()
}

def configure() {
    log.debug 'Configure'
    def bindings = getModelBindings(device.getDataValue('model'))
    def cmds = zigbee.onOffConfig() +
            zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage, DataType.UINT8, 30, 21600, 0x01) +
            zigbee.enrollResponse() +
            zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage) + bindings
    if (isHeimanButton()) {
        cmds += zigbee.writeAttribute(0x0000, 0x0012, DataType.BOOLEAN, 0x01) +
         addHubToGroup(0x000F) + addHubToGroup(0x0010) + addHubToGroup(0x0011) + addHubToGroup(0x0012)
    }
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
    log.debug 'initialize'
    def numberOfButtons = modelNumberOfButtons[device.getDataValue('model')]
    numberOfButtons = 6
    log.debug 'numberOfButtons: ' + numberOfButtons
    sendEvent(name: 'numberOfButtons', value: numberOfButtons, displayed: false)
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

private getBatteryVoltage() { 0x0020 }
private getSwitchType() { 0x0000 }
private getHoldTime() { 1000 }
private getButtonMap() {
    [
        '3450-L' : [
                '01' : 4,
                '02' : 3,
                '03' : 1,
                '04' : 2
        ],
        'lumi.remote.b686opcn01':[
                '01' : 1,
                '02' : 2,
                '03' : 3,
                '04' : 4,
                '05' : 5,
                '06' : 6
        ]
    ]
}

private getSupportedButtonValues() {
    def values
    values = ['pushed', 'held']
    return values
}

private getModelNumberOfButtons() {
    [
        '3450-L' : 4,
        'lumi.remote.b686opcn01':6
    ]
}

private getModelBindings(model) {
    def bindings = []
    for (def endpoint : 1..6) {
        bindings += zigbee.addBinding(zigbee.ONOFF_CLUSTER, ['destEndpoint' : endpoint])
    }
    bindings
}

private getButtonName() {
    def values = device.displayName.endsWith(' 1') ? "${device.displayName[0..-2]}" : "${device.displayName}"
    return values
}

/* groovylint-disable-next-line UnusedPrivateMethod */
private Map parseAduroSmartButtonMessage(Map descMap) {
    def buttonState = 'pushed'
    def buttonNumber = 0
    if (descMap.clusterInt == zigbee.ONOFF_CLUSTER) {
        if (descMap.command == '01') {
            buttonNumber = 1
        } else if (descMap.command == '00') {
            buttonNumber = 4
        }
    } else if (descMap.clusterInt == ADUROSMART_SPECIFIC_CLUSTER) {
        def list2 = descMap.data
        buttonNumber = (list2[1] as int) + 1
    }
    if (buttonNumber != 0) {
        def childevent = createEvent(name: 'button', value: 'pushed', data: [buttonNumber: 1], isStateChange: true)
        sendEventToChild(buttonNumber, childevent)
        def descriptionText = "$device.displayName button $buttonNumber was $buttonState"
        return createEvent(name: 'button', value: buttonState, data: [buttonNumber: buttonNumber], descriptionText: descriptionText, isStateChange: true)
        } else {
        return [:]
    }
}

def getADUROSMART_SPECIFIC_CLUSTER() { 0xFCCC }

private getCLUSTER_GROUPS() { 0x0004 }

private List addHubToGroup(Integer groupAddr) {
    ["st cmd 0x0000 0x01 ${CLUSTER_GROUPS} 0x00 {${zigbee.swapEndianHex(zigbee.convertToHexString(groupAddr, 4))} 00}",
     'delay 200']
}

def isHeimanButton() {
    device.getDataValue('model') == 'SceneSwitch-EM-3.0'
}
