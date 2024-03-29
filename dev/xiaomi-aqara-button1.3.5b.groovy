/**
 *  Xiaomi Aqara Zigbee Button
 *  Device Handler for SmartThings
 *  for Aqara Button models WXKG11LM (original & new revision) / WXKG12LM
 *  and Aqara Wireless Smart Light Switch models WXKG02LM / WXKG03LM (original & new revision)
 *  Version 1.3.5b
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Original device handler code by a4refillpad, adapted for use with Aqara model by bspranger
 *  Additional contributions to code by alecm, alixjg, bspranger, gn0st1c, foz333, jmagnuson, rinkek, ronvandegraaf, snalee, tmleafs, twonk, veeceeoh, & xtianpaiva
 *
 *  Notes on capabilities of the different models:
 *  Model WXKG03LM (1 button - Original revision):
 *    - Only single press is supported, sent as button 1 "pushed" event
 *  Model WXKG03LM (1 button - New revision):
 *    - Single press results in button 1 "pushed" event
 *    - Press and hold for more than 400ms results in button 1 "held" event
 *    - Double click results in button 2 "pushed" event
 *  Model WXKG02LM (2 button - Original revision):
 *    - Left, right, or both buttons pressed all result in button 1 "pushed" event
 *      This is because the SmartThings API ignores the data that distinguishes between left, right, or both-button
 *  Model WXKG02LM (2 button - New revision):
 *    - Single press of either/both buttons results in button 1 "pushed" event
 *    - Press and hold of either/both buttons for more than 400ms results in button 1 "held" event
 *    - Double click of either/both buttons results in button 2 "pushed" event
 *    - Left, right, and both button pushes are not recognized because the SmartThings API ignores the data that distinguishes between left, right, or both-button
 *      Details of button press ZigBee messages:
 *         Cluster 0012 (Multistate Input)
 *         Attribute 0055
 *         Endpoint 1 = left, 2 = right, 3 = both (ignored by SmartThings)
 *         Value 0 = hold, 1 = single, 2 = double
 *  Model WXKG11LM (original revision))
 *    - Only single press is supported, sent as button 1 "pushed" event
 *  Model WXKG11LM (new revision):
 *    - Single click results in button 1 "pushed" event
 *    - Hold for longer than 400ms results in button 1 "held" event
 *    - Double click results in button 2 "pushed" event
 *    - Single or double click results in custom "lastPressedCoRE" event for webCoRE use
 *    - Release of button results in "lastReleasedCoRE" event for webCoRE use
 *  Model WXKG12LM:
 *    - Single click results in button 1 "pushed" event
 *    - Hold for longer than 400ms results in button 1 "held" event
 *    - Double click results in button 2 "pushed" event
 *    - Shaking the button results in button 3 "pushed" event
 *    - Single or double click results in custom "lastPressedCoRE" event for webCoRE use
 *    - Release of button results in "lastReleasedCoRE" event for webCoRE use
 *
 *  Known issues:
 *    - Xiaomi sensors do not seem to respond to refresh requests
 *     - Inconsistent rendering of user interface text/graphics between iOS and Android devices - This is due to SmartThings, not this device handler
 *    - Pairing Xiaomi sensors can be difficult as they were not designed to use with a SmartThings hub.
 *    - The battery level is not reported at pairing. Wait for the first status report, 50-60 minutes after pairing.
 *
 */

metadata {
    definition (name: "Xiaomi Aqara Button", namespace: "bspranger", author: "bspranger") {
        capability "Battery"
        capability "Sensor"
        capability "Button"
        capability "Holdable Button"
        capability "Actuator"
        capability "Momentary"
        capability "Configuration"
        capability "Health Check"

        attribute "lastCheckin", "string"
        attribute "lastCheckinCoRE", "string"
        attribute "lastHeld", "string"
        attribute "lastHeldCoRE", "string"
        attribute "lastPressed", "string"
        attribute "lastPressedCoRE", "string"
        attribute "lastReleased", "string"
        attribute "lastReleasedCoRE", "string"
        attribute "batteryRuntime", "string"
        attribute "buttonStatus", "enum", ["pushed", "held", "single-clicked", "double-clicked", "shaken", "released"]

        // Aqara Button - model WXKG11LM (original revision)
        fingerprint endpointId: "01", profileId: "0104", deviceId: "5F01", inClusters: "0000,FFFF,0006", outClusters: "0000,0004,FFFF", manufacturer: "LUMI", model: "lumi.sensor_switch.aq2", deviceJoinName: "Aqara Button WXKG11LM"
        // Aqara Button - model WXKG11LM (new revision)
        fingerprint endpointId: "01", profileId: "0104", deviceId: "5F01", inClusters: "0000,0012,0003", outClusters: "0000", manufacturer: "LUMI", model: "lumi.remote.b1acn01", deviceJoinName: "Aqara Button WXKG11LM r2"
        // Aqara Button - model WXKG12LM
        fingerprint endpointId: "01", profileId: "0104", deviceId: "5F01", inClusters: "0000,0001,0006,0012", outClusters: "0000", manufacturer: "LUMI", model: "lumi.sensor_switch.aq3", deviceJoinName: "Aqara Button WXKG12LM"
        fingerprint endpointId: "01", profileId: "0104", deviceId: "5F01", inClusters: "0000,0001,0006,0012", outClusters: "0000", manufacturer: "LUMI", model: "lumi.sensor_swit", deviceJoinName: "Aqara Button WXKG12LM"
        // Aqara Smart Light Switch - single button - model WXKG03LM (Original revision)
        fingerprint endpointId: "01", profileId: "0104", deviceId: "5F01", inClusters: "0000,0003,0019,0012,FFFF", outClusters: "0000,0003,0004,0005,0019,0012,FFFF", manufacturer: "LUMI", model: "lumi.sensor_86sw1lu", deviceJoinName: "Aqara Switch WXKG03LM"
        fingerprint endpointId: "01", profileId: "0104", deviceId: "5F01", inClusters: "0000,0003,0019,0012,FFFF", outClusters: "0000,0003,0004,0005,0019,0012,FFFF", manufacturer: "LUMI", model: "lumi.sensor_86sw1", deviceJoinName: "Aqara Switch WXKG03LM"
        // Aqara Smart Light Switch - single button - model WXKG03LM (New revision)
        fingerprint endpointId: "01", profileId: "0104", deviceId: "5F01", inClusters: "0000,0003,0019,0012,FFFF", outClusters: "0000,0003,0004,0005,0019,0012,FFFF", manufacturer: "LUMI", model: "lumi.remote.b186acn01", deviceJoinName: "Aqara Switch WXKG03LM r2"
        // Aqara Smart Light Switch - dual button - model WXKG02LM (Original revision)
        fingerprint endpointId: "01", profileId: "0104", deviceId: "5F01", inClusters: "0000,0003,0019,0012,FFFF", outClusters: "0000,0003,0004,0005,0019,0012,FFFF", manufacturer: "LUMI", model: "lumi.sensor_86sw2Un", deviceJoinName: "Aqara Switch WXKG02LM"
        fingerprint endpointId: "01", profileId: "0104", deviceId: "5F01", inClusters: "0000,0003,0019,0012,FFFF", outClusters: "0000,0003,0004,0005,0019,0012,FFFF", manufacturer: "LUMI", model: "lumi.sensor_86sw2", deviceJoinName: "Aqara Switch WXKG02LM"
        // Aqara Smart Light Switch - dual button - model WXKG02LM (New revision)
        fingerprint endpointId: "01", profileId: "0104", deviceId: "5F01", inClusters: "0000,0003,0019,0012,FFFF", outClusters: "0000,0003,0004,0005,0019,0012,FFFF", manufacturer: "LUMI", model: "lumi.remote.b286acn01", deviceJoinName: "Aqara Switch WXKG02LM r2"

        command "resetBatteryRuntime"
    }

    simulator {
        status "Press button": "on/off: 0"
        status "Release button": "on/off: 1"
    }

    tiles(scale: 2) {
        multiAttributeTile(name:"buttonStatus", type: "lighting", width: 6, height: 4, canChangeIcon: false) {
            tileAttribute ("device.buttonStatus", key: "PRIMARY_CONTROL") {
                attributeState("default", label:'Pushed', backgroundColor:"#00a0dc", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/ButtonPushed.png")
                attributeState("pushed", label:'Pushed', backgroundColor:"#00a0dc", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/ButtonPushed.png")
                attributeState("held", label:'Held', backgroundColor:"#00a0dc", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/ButtonPushed.png")
                attributeState("single-clicked", label:'Single-clicked', backgroundColor:"#00a0dc", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/ButtonPushed.png")
                attributeState("double-clicked", label:'Double-clicked', backgroundColor:"#00a0dc", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/ButtonPushed.png")
                attributeState("shaken", label:'Shaken', backgroundColor:"#00a0dc", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/ButtonPushed.png")
                attributeState("released", label:'Released', action: "momentary.push", backgroundColor:"#ffffff", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/ButtonReleased.png")
            }
            tileAttribute("device.lastPressed", key: "SECONDARY_CONTROL") {
                attributeState "lastPressed", label:'Last Pressed: ${currentValue}'
            }
        }
        valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
            state "battery", label:'${currentValue}%', unit:"%", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/XiaomiBattery.png",
            backgroundColors:[
                [value: 10, color: "#bc2323"],
                [value: 26, color: "#f1d801"],
                [value: 51, color: "#44b621"]
            ]
        }
        valueTile("lastCheckin", "device.lastCheckin", decoration: "flat", inactiveLabel: false, width: 4, height: 1) {
            state "lastCheckin", label:'Last Event:\n${currentValue}'
        }
        valueTile("batteryRuntime", "device.batteryRuntime", inactiveLabel: false, decoration: "flat", width: 4, height: 1) {
            state "batteryRuntime", label:'Battery Changed: ${currentValue}'
        }
        main (["buttonStatus"])
        details(["buttonStatus","battery","lastCheckin","batteryRuntime"])
    }

    preferences {
        //Date & Time Config
        input description: "", type: "paragraph", element: "paragraph", title: "DATE & CLOCK"
        input name: "dateformat", type: "enum", title: "Set Date Format\nUS (MDY) - UK (DMY) - Other (YMD)", description: "Date Format", options:["US","UK","Other"]
        input name: "clockformat", type: "bool", title: "Use 24 hour clock?"
        //Battery Reset Config
        input description: "If you have installed a new battery, the toggle below will reset the Changed Battery date to help remember when it was changed.", type: "paragraph", element: "paragraph", title: "CHANGED BATTERY DATE RESET"
        input name: "battReset", type: "bool", title: "Battery Changed?"
        //Advanced Settings
        input description: "Only change the settings below if you know what you're doing.", type: "paragraph", element: "paragraph", title: "ADVANCED SETTINGS"
        //Battery Voltage Range
        input description: "", type: "paragraph", element: "paragraph", title: "BATTERY VOLTAGE RANGE"
        input name: "voltsmax", type: "decimal", title: "Max Volts\nA battery is at 100% at __ volts\nRange 2.8 to 3.4", range: "2.8..3.4", defaultValue: 3
        input name: "voltsmin", type: "decimal", title: "Min Volts\nA battery is at 0% (needs replacing) at __ volts\nRange 2.0 to 2.7", range: "2..2.7", defaultValue: 2.5
        //Live Logging Message Display Config
        input description: "These settings affect the display of messages in the Live Logging tab of the SmartThings IDE.", type: "paragraph", element: "paragraph", title: "LIVE LOGGING"
        input name: "infoLogging", type: "bool", title: "Display info log messages?", defaultValue: true
        input name: "debugLogging", type: "bool", title: "Display debug log messages?"
    }
}

//adds functionality to press the center tile as a virtualApp Button
def push() {
    displayInfoLog(": Virtual App Button Pressed")
    sendEvent(mapButtonEvent(3))
}

// Parse incoming device messages to generate events
def parse(description) {
    displayDebugLog(": Parsing '${description}'")
    def result = [:]

    // Any report - button press & Battery - results in a lastCheckin event and update to Last Checkin tile
    sendEvent(name: "lastCheckin", value: formatDate(), displayed: false)
    sendEvent(name: "lastCheckinCoRE", value: now(), displayed: false)

    // Send message data to appropriate parsing function based on the type of report
    if (description?.startsWith('on/off: ')) {
        // Models WXKG11LM (original revision), WXKG02LM/WXKG03LM (original revision) - button press generates pushed event
        updateLastPressed("Pressed")
        result = mapButtonEvent(3)
    } else if (description?.startsWith("read attr - raw: ")) {
        // Parse button messages of other models, or messages on short-press of reset button
        result = parseReadAttrMessage(description)
    } else if (description?.startsWith('catchall:')) {
        // Parse battery level from regular hourly announcement messages
        result = parseCatchAllMessage(description)
    }
    if (result != [:]) {
        displayDebugLog(": Creating event $result")
        return createEvent(result)
    } else
        return [:]
}

private Map parseReadAttrMessage(String description) {
    def cluster = description.split(",").find {it.split(":")[0].trim() == "cluster"}?.split(":")[1].trim()
    def attrId = description.split(",").find {it.split(":")[0].trim() == "attrId"}?.split(":")[1].trim()
    def value = description.split(",").find {it.split(":")[0].trim() == "value"}?.split(":")[1].trim()
    def data = ""
    def modelName = ""
    def model = value
    Map resultMap = [:]

    // Process models WXKG02LM/WXKG03LM (new revision), WXKG12LM, or WXKG11LM (new revision) button message
    if (cluster == "0012")
        resultMap = mapButtonEvent(Integer.parseInt(value[2..3],16))

    // Process message containing model name and/or battery voltage report
    if (cluster == "0000" && attrId == "0005")    {
        if (value.length() > 45) {
            model = value.split("01FF")[0]
            data = value.split("01FF")[1]
            if (data[4..7] == "0121") {
                def BatteryVoltage = (Integer.parseInt((data[10..11] + data[8..9]),16))
                resultMap = getBatteryResult(BatteryVoltage)
            }
        data = ", data: ${value.split("01FF")[1]}"
        }

        // Parsing the model name
        for (int i = 0; i < model.length(); i+=2) {
            def str = model.substring(i, i+2);
            def NextChar = (char)Integer.parseInt(str, 16);
            modelName = modelName + NextChar
        }
        displayDebugLog(" reported model: $modelName$data")
    }
    return resultMap
}

// Create map of values to be used for button events
private mapButtonEvent(value) {
    // Models WXKG02LM/WXKG03LM (new revision) message values: 0: hold, 1 = push, 2 = double-click,
    // WXKG11LM (new revision) message values: 0: hold, 1 = push, 2 = double-click, 255 = release
    // WXKG12LM message values: 1 = push, 2 = double-click, 16 = hold, 17 = release, 18 = shaken
    def messageType = [0: "held", 1: "single-clicked", 2: "double-clicked", 3: "pushed", 16: "held", 18: "shaken"]
    def eventType = [0: "held", 1: "pushed", 2: "pushed", 3: "pushed", 16: "held", 18: "pushed"]
    def buttonNum = [0: 1, 1: 1, 2: 2, 3: 1, 16: 1, 18: 3]
    if (value == 17 || value == 255) {
        displayInfoLog(" was released")
        updateLastPressed("Released")
        sendEvent(name: "buttonStatus", value: "released", isStateChange: true, displayed: false)
        return [:]
    } else {
        displayInfoLog(" was ${messageType[value]} (Button ${buttonNum[value]} ${eventType[value]})")
        if (value == 0 || value == 16)
            updateLastPressed("Held")
        else
            updateLastPressed("Pressed")
        sendEvent(name: "buttonStatus", value: messageType[value], isStateChange: true, displayed: false)
        if (eventType[value] == "pushed")
            runIn(1, clearButtonStatus)
        return [
            name: 'button',
            value: eventType[value],
            data: [buttonNumber: buttonNum[value]],
            descriptionText: "$device.displayName was ${messageType[value]}",
            isStateChange: true
        ]
    }
}

// on any type of button pressed update lastHeld(CoRE), lastPressed(CoRE), or lastReleased(CoRE) to current date/time
def updateLastPressed(pressType) {
    displayDebugLog(": Setting Last $pressType to current date/time")
    sendEvent(name: "last${pressType}", value: formatDate(), displayed: false)
    sendEvent(name: "last${pressType}CoRE", value: now(), displayed: false)
}

def clearButtonStatus() {
    sendEvent(name: "buttonStatus", value: "released", isStateChange: true, displayed: false)
}

// Check catchall for battery voltage data to pass to getBatteryResult for conversion to percentage report
private Map parseCatchAllMessage(String description) {
    Map resultMap = [:]
    def catchall = zigbee.parse(description)

    if (catchall.clusterId == 0x0000) {
        def MsgLength = catchall.data.size()
        // Xiaomi CatchAll does not have identifiers, first UINT16 is Battery
        if ((catchall.data.get(0) == 0x01 || catchall.data.get(0) == 0x02) && (catchall.data.get(1) == 0xFF)) {
            for (int i = 4; i < (MsgLength-3); i++) {
                if (catchall.data.get(i) == 0x21) { // check the data ID and data type
                    // next two bytes are the battery voltage
                    resultMap = getBatteryResult((catchall.data.get(i+2)<<8) + catchall.data.get(i+1))
                    break
                }
            }
        }
    }
    return resultMap
}

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
        unit: "%",
        isStateChange:true,
        descriptionText : "$device.displayName $descText"
    ]
}

private def displayDebugLog(message) {
    if (debugLogging)
        log.debug "${device.displayName}${message}"
}

private def displayInfoLog(message) {
    if (infoLogging || state.prefsSetCount < 3)
        log.info "${device.displayName}${message}"
}

//Reset the date displayed in Battery Changed tile to current date
def resetBatteryRuntime(paired) {
    def newlyPaired = paired ? " for newly paired sensor" : ""
    sendEvent(name: "batteryRuntime", value: formatDate(true))
    displayInfoLog(": Setting Battery Changed to current date${newlyPaired}")
}

// installed() runs just after a sensor is paired using the "Add a Thing" method in the SmartThings mobile app
def installed() {
    state.prefsSetCount = 0
    displayInfoLog(": Installing")
    init()
    checkIntervalEvent("")
}

// configure() runs after installed() when a sensor is paired
def configure() {
    displayInfoLog(": Configuring")
    init()
    checkIntervalEvent("configured")
    return
}

// updated() will run twice every time user presses save in preference settings page
def updated() {
    displayInfoLog(": Updating preference settings")
    if (!state.prefsSetCount)
        state.prefsSetCount = 1
    else if (state.prefsSetCount < 3)
        state.prefsSetCount = state.prefsSetCount + 1
    init()
    if (battReset){
        resetBatteryRuntime()
        device.updateSetting("battReset", false)
    }
    checkIntervalEvent("preferences updated")
    displayInfoLog(": Info message logging enabled")
    displayDebugLog(": Debug message logging enabled")
}

def init() {
    clearButtonStatus()
    if (!device.currentState('batteryRuntime')?.value)
        resetBatteryRuntime(true)
    setNumButtons()
}

def setNumButtons() {
    if (device.getDataValue("model")) {
        def modelName = device.getDataValue("model")
        def modelText = "Button WXKG12LM"
        if (!state.numButtons) {
            if (modelName.startsWith("lumi.sensor_switch.aq2")) {
                modelText = "Button WXKG11LM (original revision)"
                state.numButtons = 1
            }
            else if (modelName.startsWith("lumi.sensor_86sw2")) {
                modelText = "Wireless Smart Light Switch WXKG02LM - dual button (2016 model)"
                state.numButtons = 1
            }
            else if (modelName.startsWith("lumi.remote.b1acn01")) {
                modelText = "Button WXKG11LM (new revision)"
                state.numButtons = 2
            }
            else if (modelName.startsWith("lumi.sensor_86sw1")) {
                modelText = "Wireless Smart Light Switch WXKG03LM - single button (2016 model)"
                state.numButtons = 2
            }
            else if (modelName.startsWith("lumi.remote.b186acn01")) {
                modelText = "Wireless Smart Light Switch WXKG03LM - single button (2018 model)"
                state.numButtons = 2
            }
            else if (modelName.startsWith("lumi.remote.b286acn01")) {
                modelText = "Wireless Smart Light Switch WXKG02LM - dual button (2018 model)"
                state.numButtons = 2
            }
            else {
                state.numButtons = 3
            }
            displayInfoLog("Model is Aqara $modelText.")
            displayInfoLog("Number of buttons set to ${state.numButtons}.")
            sendEvent(name: "numberOfButtons", value: state.numButtons)
        }
    }
    else {
        displayInfoLog("Model is unknown, so number of buttons is set to default of 3.")
        sendEvent(name: "numberOfButtons", value: 3)
    }
}

private checkIntervalEvent(text) {
    // Device wakes up every 1 hours, this interval allows us to miss one wakeup notification before marking offline
    if (text)
        displayInfoLog(": Set health checkInterval when ${text}")
    sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
}

def formatDate(batteryReset) {
    def correctedTimezone = ""
    def timeString = clockformat ? "HH:mm:ss" : "h:mm:ss aa"

    // If user's hub timezone is not set, display error messages in log and events log, and set timezone to GMT to avoid errors
    if (!(location.timeZone)) {
        correctedTimezone = TimeZone.getTimeZone("GMT")
        log.error "${device.displayName}: Time Zone not set, so GMT was used. Please set up your location in the SmartThings mobile app."
        sendEvent(name: "error", value: "", descriptionText: "ERROR: Time Zone not set, so GMT was used. Please set up your location in the SmartThings mobile app.")
    }
    else {
        correctedTimezone = location.timeZone
    }
    if (dateformat == "US" || dateformat == "" || dateformat == null) {
        if (batteryReset)
            return new Date().format("MMM dd yyyy", correctedTimezone)
        else
            return new Date().format("EEE MMM dd yyyy ${timeString}", correctedTimezone)
    }
    else if (dateformat == "UK") {
        if (batteryReset)
            return new Date().format("dd MMM yyyy", correctedTimezone)
        else
            return new Date().format("EEE dd MMM yyyy ${timeString}", correctedTimezone)
    }
    else {
        if (batteryReset)
            return new Date().format("yyyy MMM dd", correctedTimezone)
        else
            return new Date().format("EEE yyyy MMM dd ${timeString}", correctedTimezone)
    }
}