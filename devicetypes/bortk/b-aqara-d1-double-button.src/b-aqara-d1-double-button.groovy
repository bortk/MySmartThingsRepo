/* groovylint-disable CatchException, CompileStatic, CouldBeElvis, DuplicateListLiteral, DuplicateNumberLiteral, DuplicateStringLiteral, GetterMethodCouldBeProperty, ImplicitClosureParameter, InvertedIfElse, LineLength, MethodParameterTypeRequired, MethodReturnTypeRequired, NoDef, PublicMethodsBeforeNonPublicMethods, TernaryCouldBeElvis, UnnecessaryElseStatement, UnnecessaryGetter, UnnecessarySubstring, UnusedImport, VariableName, VariableTypeRequired */
/**
 *  Aqara D1 2-button Light Switch (WXKG07LM) - 2020
 */

 import groovy.json.JsonOutput
 import physicalgraph.zigbee.zcl.DataType

metadata {
    definition(name: 'B Aqara D1 Double Button', namespace: 'bortk', author: 'bortk', ocfDeviceType: 'x.com.st.d.remotecontroller') {
        capability 'Battery'
        capability 'Sensor'
        capability 'Button'
        capability 'Holdable Button'
        capability 'Actuator'
        capability 'Momentary'
        capability 'Configuration'
        capability 'Health Check'

        attribute 'lastHeld', 'string'
        attribute 'lastPressed', 'string'
        attribute 'lastReleased', 'string'
        attribute 'batteryRuntime', 'string'

        fingerprint deviceJoinName: 'Aqara D1 Double Button', model: 'lumi.remote.b286acn02',  inClusters: '0000,0003,0019,FFFF,0012', outClusters: '0000,0004,0003,0005,0019,FFFF,0012', manufacturer: 'LUMI', profileId: '0104', endpointId: '01'

        command 'resetBatteryRuntime'
    }

    preferences {
        input name: 'reloadConfig', type: 'bool', title: 'Reload Config?'
        input name: 'deleteChildren', type: 'bool', title: 'Delete Child Devices?'

        //Live Logging Message Display Config
        input description: 'These settings affect the display of messages in the Live Logging tab of the SmartThings IDE.', type: 'paragraph', element: 'paragraph', title: 'Live Logging'
        input name: 'infoLogging', type: 'bool', title: 'Log info messages?', defaultValue: true
        input name: 'debugLogging', type: 'bool', title: 'Log debug messages?', defaultValue: true
        //Battery Reset Config
        input description: 'If you have installed a new battery, the toggle below will reset the Changed Battery date to help remember when it was changed.', type: 'paragraph', element: 'paragraph', title: 'CHANGED BATTERY DATE RESET'
        input name: 'battReset', type: 'bool', title: 'Battery Changed?'
        //Advanced Settings
        input description: "Only change the settings below if you know what you're doing.", type: 'paragraph', element: 'paragraph', title: 'ADVANCED SETTINGS'
        //Battery Voltage Range
        input description: '', type: 'paragraph', element: 'paragraph', title: 'BATTERY VOLTAGE RANGE'
        input name: 'voltsmax', type: 'decimal', title: 'Max Volts\nA battery is at 100% at __ volts\nRange 2.8 to 3.4', range: '2.8..3.4', defaultValue: 3
        input name: 'voltsmin', type: 'decimal', title: 'Min Volts\nA battery is at 0% (needs replacing) at __ volts\nRange 2.0 to 2.7', range: '2..2.7', defaultValue: 2.5
    }
}

def getNUM_OF_BUTTONS() { 3 }

//adds functionality to press the center tile as a virtualApp Button
def push() {
    displayDebugLog(': push() triggered')
    def result =  [
        name: 'button',
        value: 'pushed',
        data: [buttonNumber: 0],
        descriptionText: 'Momentary tile was pushed',
        isStateChange: true
    ]
    displayDebugLog(": Sending event $result")
    sendEvent(result)
    ping()
    refresh()
    zigbee.readAttribute(0x0001, 0x0020)
}

// Parse incoming device messages to generate events
def parse(description) {
    displayDebugLog(": Parsing '$description'")
    def result = parseAttrMessage(description)
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

private getButtonName() {
    def values = device.displayName.endsWith(' 1') ? "${device.displayName[0..-2]}" : "${device.displayName}"
    return values
}

private addChildButtons(numberOfButtons) {
    displayDebugLog(": addChildButtons numberOfButtons : $numberOfButtons")
    def labels = ['Left', 'Right', 'Both']
    displayDebugLog(": addChildButtons labels : $labels")
    for (def endpoint : 1..numberOfButtons) {
        try {
            displayDebugLog(":$endpoint: addChildButtons endpoint : $endpoint")
            String childDni = "${device.deviceNetworkId}:$endpoint"
            displayDebugLog(":$endpoint:  addChildButtons childDni : $childDni")
            def componentLabel = getButtonName() + "${endpoint}"
            displayDebugLog(":$endpoint: addChildButtons componentLabel : $componentLabel")

            def child = addChildDevice('smartthings', 'Child Button', childDni, device.getHub().getId(), [
                    completedSetup: true,
                    label         : componentLabel,
                    isComponent   : true,
                    componentName : "button$endpoint",
                    componentLabel: "Button $endpoint"
            ])
            displayDebugLog( ":$endpoint: button: ${endpoint}  created")
            displayDebugLog(":$endpoint: labels[endpoint - 1] ${labels[endpoint - 1]} ")
            displayDebugLog(":$endpoint: child: ${child}  created")
            child.sendEvent(name: 'supportedButtonValues', value: supportedButtonValues.encodeAsJSON(), displayed: false)
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
    def bindings = []
    for (def endpoint : 1..NUM_OF_BUTTONS) {
        bindings += zigbee.addBinding(zigbee.ONOFF_CLUSTER, ['destEndpoint' : endpoint])
    }
    bindings
}

// on any type of button pressed update lastHeld(CoRE), lastPressed(CoRE), or lastReleased(CoRE) to current date/time
def updateLastPressed(pressType) {
    displayDebugLog(": Setting Last $pressType to current date/time")
    sendEvent(name: "last${pressType}", value: formatDate(), displayed: false)
    sendEvent(name: "last${pressType}CoRE", value: now(), displayed: false)
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
//Reset the date displayed in Battery Changed tile to current date
def resetBatteryRuntime(paired) {
    def newlyPaired = paired ? ' for newly paired sensor' : ''
    sendEvent(name: 'batteryRuntime', value: formatDate(true))
    displayInfoLog(": Setting Battery Changed to current date${newlyPaired}")
}

// installed() runs just after a sensor is paired using the "Add a Thing" method in the SmartThings mobile app
def installed() {
    // state.prefsSetCount = 0
    displayInfoLog(': Installing')
    checkIntervalEvent('')
}

// configure() runs after installed() when a sensor is paired
def configure() {
    displayInfoLog(': Configuring')
    initialize(true)
    checkIntervalEvent('configured')
    def batteryVoltage = 0x21
    def bindings = getModelBindings()
    def cmds = zigbee.onOffConfig() +
            zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage, DataType.UINT8, 30, 21600, 0x01) +
            zigbee.enrollResponse() +
            zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage) + bindings
    return cmds
}

// updated() will run every time user changes preference in the settings page
def updated() {
    displayInfoLog(': Updating preference settings')
    // if (!state.prefsSetCount) {
    //     state.prefsSetCount = 1
    // }
    // else if (state.prefsSetCount < 3) {
    //     state.prefsSetCount = state.prefsSetCount + 1
    // }

    if (deleteChildren) {
        displayDebugLog(': Deleting child devices')
        device.updateSetting('deleteChildren', false)
        childDevices.each {
            try {
                displayDebugLog(": deleting  child ${it.deviceNetworkId}")
                deleteChildDevice(it.deviceNetworkId)
                displayDebugLog(": deleted child ${it.deviceNetworkId}")
            }
            catch (e) {
                log.debug "Error deleting ${it.deviceNetworkId}: ${e}"
            }
        }
    }
    initialize(false)
    if (battReset) {
        resetBatteryRuntime()
        device.updateSetting('battReset', false)
    }
    checkIntervalEvent('preferences updated')
    displayInfoLog(': Info logging enabled')
    displayDebugLog(': Debug logging enabled')
}

def initialize(newlyPaired) {
    sendEvent(name: 'DeviceWatch-Enroll', value: JsonOutput.toJson([protocol: 'zigbee', scheme:'untracked']), displayed: false)
    sendEvent(name: 'supportedButtonValues', value: supportedButtonValues.encodeAsJSON(), displayed: false)

    if (!device.currentState('batteryRuntime')?.value) {
        resetBatteryRuntime(newlyPaired)
    }
    state.numButtons = NUM_OF_BUTTONS
    displayInfoLog(": Number of buttons set to ${state.numButtons}.")
    sendEvent(name: 'numberOfButtons', value: state.numButtons, displayed: false)

    if (!childDevices) {
        addChildButtons(state.numButtons)
    }
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
    def timeString = clockformat ? 'HH:mm:ss' : 'h:mm:ss aa'

    // If user's hub timezone is not set, display error messages in log and events log, and set timezone to GMT to avoid errors
    if (!(location.timeZone)) {
        correctedTimezone = TimeZone.getTimeZone('GMT')
        log.error "${device.displayName}: Time Zone not set, so GMT was used. Please set up your location in the SmartThings mobile app."
        sendEvent(name: 'error', value: '', descriptionText: 'ERROR: Time Zone not set, so GMT was used. Please set up your location in the SmartThings mobile app.')
    }
    else {
        correctedTimezone = location.timeZone
    }

    if (batteryReset) {
        return new Date().format('dd MMM yyyy', correctedTimezone)
    }
    else {
        return new Date().format("EEE dd MMM yyyy ${timeString}", correctedTimezone)
    }
}

def ping() {
    displayDebugLog(': ping()')
    def result = zigbee.readAttribute(0x0001, 0x0020) // Read the Battery Level
    displayDebugLog(": ping() result = ${result}")
    return result
}

def refresh() {
    displayDebugLog(': refresh()')

    def manufacturer = device.getDataValue('manufacturer')
    displayDebugLog(": refresh() manufacturer = ${manufacturer}")
    def cmds

    def result = zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0021, [destEndpoint: 0x01])
    cmds = result
    displayDebugLog(": refresh() result = ${result}")
    /* groovylint-disable-next-line DuplicateMapLiteral */
    result = zigbee.readAttribute(0x0402, 0x0000, [destEndpoint: 0x01])

    displayDebugLog(": refresh() result = ${result}")
    // descMap = zigbee.parseDescriptionAsMap(result)
    // displayDebugLog(": refresh() descMap = ${descMap}")

    result =  zigbee.readAttribute(0x0405, 0x0000, [destEndpoint: 0x02])
    displayDebugLog(": refresh() result = ${result}")

    result = zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0020)

    cmds += result
    displayDebugLog(": refresh() result = ${result}")

    displayDebugLog(': refresh() END')
    return cmds
}

def healthPoll() {
    displayDebugLog(': healthPoll()')
    def cmds = refresh()
    cmds.each { sendHubCommand(new physicalgraph.device.HubAction(it)) }
}
def poll() {
    displayDebugLog(': poll()')
}
