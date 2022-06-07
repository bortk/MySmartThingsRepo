/* groovylint-disable CatchException, DuplicateNumberLiteral, DuplicateStringLiteral, ImplicitClosureParameter, LineLength, MethodParameterTypeRequired, MethodReturnTypeRequired, NoDef, PublicMethodsBeforeNonPublicMethods, TernaryCouldBeElvis, UnnecessaryGetter, UnusedImport, VariableName, VariableTypeRequired */
/**
 *  Aqara Wireless Smart Light Switch model WXKG07LM (2020 revision)
 *  Device Handler for SmartThings
 *  Version 0.9.1
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Based on original device handler code by a4refillpad, adapted by bspranger, then rewritten and updated for changes in firmware 25.20 by veeceeoh
 *  Additional contributions to code by alecm, alixjg, bspranger, gn0st1c, foz333, jmagnuson, rinkek, ronvandegraaf, snalee, tmleafs, twonk, veeceeoh, & xtianpaiva
 *
 *  Notes on capabilities of the different models:
 *  Aqara D1 2-button Light Switch (WXKG07LM) - 2020 (lumi.remote.b286acn02):
 *
 *  Known issues:
 *  - As of March 2019, the SmartThings Samsung Connect mobile app does NOT support custom device handlers such as this one
 *  - The SmartThings Classic mobile app UI text/graphics is rendered differently on iOS vs Android devices - This is due to SmartThings, not this device handler
 *  - Pairing Xiaomi/Aqara devices can be difficult as they were not designed to use with a SmartThings hub.
 *  - The battery level is not reported at pairing. Wait for the first status report, 50-60 minutes after pairing.
 *  - Xiaomi devices do not respond to refresh requests
 *  - Most ZigBee repeater devices (generally mains-powered ZigBee devices) are NOT compatible with Xiaomi/Aqara devices, causing them to drop off the network.
 *    Only XBee ZigBee modules, the IKEA Tradfri Outlet / Tradfri Bulb, and ST user @iharyadi's custom multi-sensor ZigBee repeater device are confirmed to be compatible.
 */

 import groovy.json.JsonOutput
 import physicalgraph.zigbee.zcl.DataType

/* groovylint-disable-next-line CompileStatic */
metadata {
    /* groovylint-disable-next-line SpaceAfterMethodCallName */
    definition (name: 'B Xiaomi Aqara Wireless Switch', namespace: 'bortk', author: 'SmartThings', mcdSync: true,, ocfDeviceType: 'x.com.st.d.remotecontroller') {
        capability 'Battery'
        capability 'Sensor'
        capability 'Button'
        capability 'Holdable Button'
        capability 'Actuator'
        // capability 'Momentary'
        capability 'Configuration'
        capability 'Health Check'

        //capability 'Refresh'

        attribute 'lastCheckin', 'string'
        attribute 'lastCheckinCoRE', 'string'
        attribute 'lastHeld', 'string'
        attribute 'lastHeldCoRE', 'string'
        attribute 'lastPressed', 'string'
        attribute 'lastPressedCoRE', 'string'
        attribute 'lastReleased', 'string'
        attribute 'lastReleasedCoRE', 'string'
        attribute 'batteryRuntime', 'string'
        attribute 'buttonStatus', 'enum', ['pushed', 'held', 'single-clicked', 'double-clicked', 'shaken', 'released']

        fingerprint deviceJoinName: 'Aqara D1 2-button Light Switch (WXKG07LM) - 2020', model: 'lumi.remote.b286acn02',  inClusters: '0000,0003,0019,FFFF,0012', outClusters: '0000,0004,0003,0005,0019,FFFF,0012', manufacturer: 'LUMI', profileId: '0104', endpointId: '01'

        command 'resetBatteryRuntime'
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

    simulator {
        status 'Press button': 'on/off: 0'
        status 'Release button': 'on/off: 1'
    }

    preferences {
        input name: 'reloadSettings', type: 'bool', title: 'Reload Settings'
        //Date & Time Config
        input description: '', type: 'paragraph', element: 'paragraph', title: 'DATE & CLOCK'
        input name: 'clockformat', type: 'bool', title: 'Use 24 hour clock?'
        //Battery Reset Config
        input description: 'If you have installed a new battery, the toggle below will reset the Changed Battery date to help remember when it was changed.', type: 'paragraph', element: 'paragraph', title: 'CHANGED BATTERY DATE RESET'
        input name: 'battReset', type: 'bool', title: 'Battery Changed?'
        //Advanced Settings
        input description: "Only change the settings below if you know what you're doing.", type: 'paragraph', element: 'paragraph', title: 'ADVANCED SETTINGS'
        //Battery Voltage Range
        input description: '', type: 'paragraph', element: 'paragraph', title: 'BATTERY VOLTAGE RANGE'
        input name: 'voltsmax', type: 'decimal', title: 'Max Volts\nA battery is at 100% at __ volts\nRange 2.8 to 3.4', range: '2.8..3.4', defaultValue: 3
        input name: 'voltsmin', type: 'decimal', title: 'Min Volts\nA battery is at 0% (needs replacing) at __ volts\nRange 2.0 to 2.7', range: '2..2.7', defaultValue: 2.5
        //Live Logging Message Display Config
        input description: 'These settings affect the display of messages in the Live Logging tab of the SmartThings IDE.', type: 'paragraph', element: 'paragraph', title: 'LIVE LOGGING'
        input name: 'infoLogging', type: 'bool', title: 'Display info log messages?', defaultValue: true
        input name: 'debugLogging', type: 'bool', title: 'Display debug log messages?', defaultValue: true
    }
}

//adds functionality to press the center tile as a virtualApp Button
def push() {
    def result = mapButtonEvent(0, 1)
    debugLog(": Sending event $result")
    sendEvent(result)
}

// Parse incoming device messages to generate events
def parse(description) {
    debugLog(': parse (description)', description)
    def result = [:]

    // Any report - button press & Battery - results in a lastCheckin event and update to Last Checkin tile
    sendEvent(name: 'lastCheckin', value: formatDate(), displayed: false)
    sendEvent(name: 'lastCheckinCoRE', value: now(), displayed: false)

    // Send message data to appropriate parsing function based on the type of report
    if (description?.startsWith('on/off: ')) {
        // Hub FW prior to 25.x - Models WXKG02LM/WXKG03LM (original revision) - any press generates button 1 pushed event
        state.numButtons = 1
        result = mapButtonEvent(1, 1)
    } else if (description?.startsWith('read attr')) {
        // Parse button messages of other models, or messages on short-press of reset button
        result = parseReadAttrMessage(description - 'read attr - ')
    } else if (description?.startsWith('catchall')) {
        debugLog(": Manual Parsing catchall '$description'")
        /* groovylint-disable-next-line ParameterReassignment */
        description = description - 'catchall: '
        debugLog(": TRIM: '$description'")
        result = parseReadAttrMessageNew(description)
    // Parse battery level from regular hourly announcement messages
    //result = parseCatchAllMessage(description)
    }
    if (result != [:]) {
        debugLog(": Creating event $result")
        return createEvent(result)
    }
    return [:]
}

private Map parseReadAttrMessageNew(String description) {
    debugLog(": parseReadAttrMessageNew: '$description'")
    Map descMap = (description).split(' ').inject([:]) {
        map, param ->
        def nameAndValue = param.split(':')
        map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
    }
    debugLog(": map: '$map'")
    Map resultMap = [:]
    if (descMap.cluster == '0006') {
        // Process model WXKG02LM / WXKG03LM (2016 revision) button messages
        resultMap = mapButtonEvent(Integer.parseInt(descMap.endpoint, 16), 1)
    } else if (descMap.cluster == '0012') {
        // Process model WXKG02LM / WXKG03LM (2018 revision) button messages
        resultMap = mapButtonEvent(Integer.parseInt(descMap.endpoint, 16), Integer.parseInt(descMap.value[2..3], 16))
    } else if (descMap.cluster == '0000' && descMap.attrId == '0005')    {
        // Process message containing model name and/or battery voltage report
        def data = ''
        if (descMap.value.length() > 45) {
            model = descMap.value.split('01FF')[0]
            data = descMap.value.split('01FF')[1]
            if (data[4..7] == '0121') {
                def BatteryVoltage = (Integer.parseInt((data[10..11] + data[8..9]), 16))
                resultMap = getBatteryResult(BatteryVoltage)
            }
        }
    }
    return resultMap
}
private Map parseReadAttrMessage(String description) {
    debugLog(": parseReadAttrMessage: '$description'")
    Map descMap = (description).split(',').inject([:]) {
        map, param ->
        def nameAndValue = param.split(':')
        map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
    }
    debugLog(": map : '$map'")
    Map resultMap = [:]
    if (descMap.cluster == '0006') {
        // Process model WXKG02LM / WXKG03LM (2016 revision) button messages
        resultMap = mapButtonEvent(Integer.parseInt(descMap.endpoint, 16), 1)
    } else if (descMap.cluster == '0012') {
        // Process model WXKG02LM / WXKG03LM (2018 revision) button messages
        resultMap = mapButtonEvent(Integer.parseInt(descMap.endpoint, 16), Integer.parseInt(descMap.value[2..3], 16))
    } else if (descMap.cluster == '0000' && descMap.attrId == '0005')    {
        // Process message containing model name and/or battery voltage report
        def data = ''
        def modelName = ''
        def model = descMap.value
        if (descMap.value.length() > 45) {
            model = descMap.value.split('01FF')[0]
            data = descMap.value.split('01FF')[1]
            if (data[4..7] == '0121') {
                def BatteryVoltage = (Integer.parseInt((data[10..11] + data[8..9]), 16))
                resultMap = getBatteryResult(BatteryVoltage)
            }
        }
        // Parsing the model name
        for (int i = 0; i < model.length(); i += 2) {
            /* groovylint-disable-next-line UnnecessarySubstring */
            def str = model.substring(i, i + 2)
            def NextChar = (char)Integer.parseInt(str, 16)
            modelName = modelName + NextChar
        }
        debugLog(' reported ZigBee model ', $modelName)
    }
    return resultMap
}

// Create map of values to be used for button events
private mapButtonEvent(buttonValue, actionValue) {
    // buttonValue (message endpoint) 1 = left, 2 = right, 3 = both (and 0 = virtual app button)
    // actionValue (message value) 0 = hold, 1 = push, 2 = double-click (hold & double-click on 2018 revision only)
    def whichButtonText = ['Virtual button was', ((state.numButtons < 3) ? 'Button was' : 'Left button was'), 'Right button was', 'Both buttons were']
    def statusButton = ['', ((state.numButtons < 3) ? '' : 'left'), 'right', 'both']
    def pressType = ['held', 'pushed', 'double-clicked']
    def eventType = (actionValue == 0) ? 'held' : 'pushed'
    def lastPressType = (actionValue == 0) ? 'Held' : 'Pressed'
    def buttonNum = (buttonValue == 0 ? 1 : buttonValue) + (actionValue == 2 ? 3 : 0)
    def descText = "${whichButtonText[buttonValue]} ${pressType[actionValue]} (Button $buttonNum $eventType)"
    sendEvent(name: 'buttonStatus', value: "${statusButton[buttonValue]}${pressType[actionValue]}", isStateChange: true, displayed: false)
    updateLastPressed(lastPressType)
    displayInfoLog(": $descText")
    runIn(1, clearButtonStatus)
    return [
        name: 'button',
        value: eventType,
        data: [buttonNumber: buttonNum],
        descriptionText: descText,
        isStateChange: true
    ]
}

// on any type of button pressed update lastHeld(CoRE), lastPressed(CoRE), or lastReleased(CoRE) to current date/time
def updateLastPressed(pressType) {
    debugLog(": Setting Last $pressType to current date/time")
    sendEvent(name: "last${pressType}", value: formatDate(), displayed: false)
    sendEvent(name: "last${pressType}CoRE", value: now(), displayed: false)
}

def clearButtonStatus() {
    sendEvent(name: 'buttonStatus', value: 'released', isStateChange: true, displayed: false)
}

// Check catchall for battery voltage data to pass to getBatteryResult for conversion to percentage report
/*
private Map parseCatchAllMessage(String description) {
    debugLog(": parseCatchAllMessage: $description ")
    Map resultMap = [:]
    def catchall = zigbee.parse(description)
    debugLog(": catchall: $catchall ")
    if (catchall.clusterId == 0x0000) {
        def MsgLength = catchall.data.size()
        // Xiaomi CatchAll does not have identifiers, first UINT16 is Battery
        if ((catchall.data.get(0) == 0x01 || catchall.data.get(0) == 0x02) && (catchall.data.get(1) == 0xFF)) {
            for (int i = 4; i < (MsgLength - 3); i++) {
                if (catchall.data.get(i) == 0x21) { // check the data ID and data type
                    // next two bytes are the battery voltage
                    resultMap = getBatteryResult((catchall.data.get(i + 2) << 8) + catchall.data.get(i + 1))
                    break
                }
            }
        }
    }
    return resultMap
}*/

// Convert raw 4 digit integer voltage value into percentage based on minVolts/maxVolts range
private Map getBatteryResult(rawValue) {
    // raw voltage is normally supplied as a 4 digit integer that needs to be divided by 1000
    // but in the case the final zero is dropped then divide by 100 to get actual voltage value
    def rawVolts = rawValue / 1000
    def minVolts = voltsmin ? voltsmin : 2.5
    def maxVolts = voltsmax ? voltsmax : 3.0
    def pct = (rawVolts - minVolts) / (maxVolts - minVolts)
    def roundedPct = Math.min(100, Math.round(pct * 100))
    def descText = "Battery at ${roundedPct}% (${rawVolts} Volts)"
    displayInfoLog(": $descText")
    return [
        name: 'battery',
        value: roundedPct,
        unit: '%',
        isStateChange:true,
        descriptionText : "$device.displayName $descText"
    ]
}

private debugLog(message) {
    if (debugLogging) {
        log.debug "${device.displayName}${message}"
    }
}
private debugLog(message, param) {
    if (debugLogging) {
        log.debug "${device.displayName}${message}: $param"
    }
}

private displayInfoLog(message) {
    // if (infoLogging || state.prefsSetCount < 3) {
    log.info "${device.displayName}${message}"
// }
}

//Reset the date displayed in Battery Changed tile to current date
def resetBatteryRuntime(paired) {
    def newlyPaired = paired ? ' for newly paired sensor' : ''
    sendEvent(name: 'batteryRuntime', value: formatDate(true))
    displayInfoLog(": Setting Battery Changed to current date${newlyPaired}")
}

// installed() runs just after a sensor is paired using the "Add a Thing" method in the SmartThings mobile app
def installed() {
    state.prefsSetCount = 0
    displayInfoLog(': Installing')
    checkIntervalEvent('')
    sendEvent(name: 'button', value: 'pushed', isStateChange: true, displayed: false)
    sendEvent(name: 'supportedButtonValues', value: supportedButtonValues.encodeAsJSON(), displayed: false)
    initialize()
}

// configure() runs after installed() when a sensor is paired
def configure() {
    displayInfoLog(': Configuring')

    def bindings = getModelBindings()
    def cmds = zigbee.onOffConfig() +
            zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage, DataType.UINT8, 30, 21600, 0x01) +
            zigbee.enrollResponse() +
            zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage) + bindings
    return cmds
// Old Aqara code
// initialize(true)
// checkIntervalEvent('configured')
// return
}

// updated() will run twice every time user presses save in preference settings page
def updated() {
    displayInfoLog(': Updating preference settings')
    /* groovylint-disable-next-line CouldBeElvis */
    if (!state.prefsSetCount) {
        state.prefsSetCount = 1
    }
    else if (state.prefsSetCount < 3) {
        state.prefsSetCount = state.prefsSetCount + 1
    }
    initialize(false)
    if (battReset) {
        resetBatteryRuntime()
        device.updateSetting('battReset', false)
    }
    checkIntervalEvent('preferences updated')
    displayInfoLog(': Info message logging enabled')
    debugLog(': Debug message logging enabled')
}

def initialize(newlyPaired) {
    debugLog('initialize')
    sendEvent(name: 'DeviceWatch-Enroll', value: JsonOutput.toJson([protocol: 'zigbee', scheme:'untracked']), displayed: false)
    clearButtonStatus()
    if (!device.currentState('batteryRuntime')?.value) {
        resetBatteryRuntime(newlyPaired)
    }
    setNumButtons()

    debugLog('state.numButtons: ' + state.numButtons)

    sendEvent(name: 'numberOfButtons', value: state.numButtons, isStateChange: false)
    sendEvent(name: 'checkInterval', value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: 'zigbee', hubHardwareId: device.hub.hardwareID])

    if (!childDevices) {
        addChildButtons(state.numButtons)
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

def setNumButtons() {
    state.numButtons = 2
    displayInfoLog(': Model is Aqara Aqara D1 2-button Light Switch (WXKG07LM)')
    displayInfoLog(": Number of buttons set to ${state.numButtons}.")
    sendEvent(name: 'numberOfButtons', value: state.numButtons, displayed: false)
// device.currentValue('numberOfButtons')?.times {
//             sendEvent(name: 'button', value: 'pushed', data: [buttonNumber: it + 1], displayed: false)
// }
}

def sendEventToChild(buttonNumber, event) {
    String childDni = "${device.deviceNetworkId}:$buttonNumber"
    def child = childDevices.find { it.deviceNetworkId == childDni }
    child?.sendEvent(event)
}

private checkIntervalEvent(text) {
    // Device wakes up every 1 hours, this interval allows us to miss one wakeup notification before marking offline
    if (text) {
        displayInfoLog(": Set health checkInterval when ${text}")
        sendEvent(name: 'checkInterval', value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: 'zigbee', hubHardwareId: device.hub.hardwareID])
    }
}

def formatDate(batteryReset) {
    def correctedTimezone = ''
    if (location.timeZone) {
        correctedTimezone = location.timeZone
    }
    else {
        correctedTimezone = TimeZone.getTimeZone('GMT')
        log.error "${device.displayName}: Time Zone not set, so GMT was used. Please set up your location in the SmartThings mobile app."
        sendEvent(name: 'error', value: '', descriptionText: 'ERROR: Time Zone not set, so GMT was used. Please set up your location in the SmartThings mobile app.')
    }

    def timeString = clockformat ? 'HH:mm:ss' : 'h:mm:ss aa'
    if (batteryReset) {
        return new Date().format('dd MMM yyyy', correctedTimezone)
    }
    /* groovylint-disable-next-line UnnecessaryElseStatement */
    else {
        return new Date().format("EEE dd MMM yyyy ${timeString}", correctedTimezone)
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
