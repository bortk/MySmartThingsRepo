/**
 *  ShellyLANWindowShadeDH
 *
 *  Copyright 2020 Boris Tsirulnik
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
metadata {
	definition (name: "ShellyLANWindowShadeDH", namespace: "bortk-devac", author: "Boris Tsirulnik",  ocfDeviceType: "oic.d.blind", vid: "generic-shade", cstHandler: true) {
		capability "Contact Sensor"
		capability "Switch"
		capability "Switch Level"
		capability "Window Shade"
		capability "Window Shade Level"
		capability "Window Shade Preset"
	}


	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
		// TODO: define your main and details tiles here
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'contact' attribute
	// TODO: handle 'switch' attribute
	// TODO: handle 'level' attribute
	// TODO: handle 'windowShade' attribute
	// TODO: handle 'supportedWindowShadeCommands' attribute
	// TODO: handle 'shadeLevel' attribute

}

// handle commands
def on() {
	log.debug "Executing 'on'"
	// TODO: handle 'on' command
}

def off() {
	log.debug "Executing 'off'"
	// TODO: handle 'off' command
}

def setLevel() {
	log.debug "Executing 'setLevel'"
	// TODO: handle 'setLevel' command
}

def open() {
	log.debug "Executing 'open'"
	// TODO: handle 'open' command
}

def close() {
	log.debug "Executing 'close'"
	// TODO: handle 'close' command
}

def pause() {
	log.debug "Executing 'pause'"
	// TODO: handle 'pause' command
}

def setShadeLevel() {
	log.debug "Executing 'setShadeLevel'"
	// TODO: handle 'setShadeLevel' command
}

def presetPosition() {
	log.debug "Executing 'presetPosition'"
	// TODO: handle 'presetPosition' command
}


def refresh() {
    log.debug "Refresh - Getting Status"
    sendHubCommand(new physicalgraph.device.HubAction(
      method: "GET",
      path: "/roller/0",
      headers: [
        HOST: getShellyAddress(),
        "Content-Type": "application/x-www-form-urlencoded"
      ]
    ))
}

def sendRollerCommand(action) {
    log.debug "Calling /roller/0 with $action"
    sendHubCommand(new physicalgraph.device.HubAction(
      method: "POST",
      path: "/roller/0",
      body: action,
      headers: [
        HOST: getShellyAddress(),
        "Content-Type": "application/x-www-form-urlencoded"
      ]
    ))
    runIn(25, refresh)
}


private getShellyAddress() {
    def port = 80
    def iphex = ip.tokenize( '.' ).collect { String.format( '%02x', it.toInteger() ) }.join().toUpperCase()
    def porthex = String.format('%04x', port.toInteger())
    def shellyAddress = iphex + ":" + porthex
    device.deviceNetworkId = shellyAddress.toUpperCase()
    log.debug "Using IP " + ip + ", PORT 80 and HEX ADDRESS " + shellyAddress + " for device: ${device.id}"
    return device.deviceNetworkId
}