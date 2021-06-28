# 2021 overhaul


## Problems to solve

* Ethernet is a PITA and it has been unreliable in the past.
* We need rs485 to scale to many more devices
* We need to support longer wiegand codes than the old system.
* Logging has been unreliable, due to unreliable ethernet.
* De-centralized operation without logging is not desirable.


This boils down to: I have to change the software quite a bit, so I might as well redo the complete firmware in a simpler way.

## New plan:

* Make the door controller almost entirely stateless and very dumb.
* Buffer (wiegand|gpio) input events in a queue.
* Respond to polls / output commands from the server.
* All rs485 communication to be AES encrypted with a pre-shared key, which is picked by the server during enrollment.


## Enrollment

* All devices start out un-enrolled.
* To reset an enrolled device to unenrolled status, hold the factory-reset button during power-up.
* when unenrolled, the device responds to enrollment requests, the server periodically polls for enrolling devices.
* It's assumed that the server and the device share a secure rs485 bus during enrollment.
* Enrollment means sending the shared AES key and node ID to the device from the server.
* The AES key and node id are stored in EEPROM in the controller.
* Once enrolled the device is added to the servers list of unassigned devices and the server will start polling it, but no operations other than logging will take place with the device.
* To actually use the device, the server must be configured to use the device.


# Protocol

All datagrams have the overall format, this is called a Frame:

| Size   | Meaning |
| ------ |: ------:|
| 1      | Start of message, always 0xf0 |
| 1      | source id (0x00 is controller, 0xff is discovery) |
| 1      | target id (0x00 is controller, 0xff is discovery) |
| 1      | message type |
| 1      | psize=payload size |
| psize  | (encrypted) payload |
| 4      | CRC32 of all previous bytes, except for the start-of-message byte
| 1      | End of message, always 0xf1 |

Only the adressed device may answer a request and it must do so within 5 ms of receiving a message.

The controller will poll all configured devices in loop with maximum time between sending a request and proceeding to the next device of 100 ms.


## Poll: 0x00

Sent by controller to each known device in turn.

Once per round it's also sent to target id 0xff to poll for enrollment requests.

Payload: 1 byte with the last event received by the controller from the device.


The device will answer with either an acknowlege message, an enrollment request or a poll response.


## Enrollment request: 0x01

The device sleeps a random interval for 0-5 ms before sending the response.

While sleeping it will listen for bytes on the bus, only if none are seen will it transmit.

Payload: 4 bytes.

The payload contains 4 random bytes, this is used to guard against collisions as any conflicts will cause the CRC to not match.

When the controller sees an enrollment request it picks a new device id and a device specific AES key, it then responds with an enrollment response message.


## Enrollment response: 0x02

Sent by the controller.

The target id is set to 0xff and the payload contains:

* The 4 random bytes from the request
* The node ID
* The 32 bytes of the AES key.

The device stores the device id and the AES key in its EEPROM and awaits a poll on its new ID.

This message is repeated until the device answers


## Poll acknowlege: 0x03

Sent by the device whenever a poll is received that the controller does not have an immediate answer for.

No payload.


## Poll response: 0x04

Sent by the device as a response to poll requests when responding to a poll.

Payload is an encrypted list of events.


## Encrypted payload

| Size | Name | Meaning | 
| ---- |: ---:| -------:|
| 16   | IV | Initiranalization vector, chosen by random for each message | 
| 1    | Size | The number of encrypted bytes, this can be smaller than the payload says due to padding |
| size | Data | The encrypted data, padded to 16 byte blocks |

## Decrypted payload

The plain text payload contains the actual data and a crc32 of the data to prevent modification:

| Size | Name | Meaning | 
| ---- |: ---:| -------:|
| size | Data | The plain text data, padded to 16 byte blocks |
| 4    | CRC32 | CRC32 of the plain text data |

## Event payload

The data of an event payload consists of 0 to many events

### Events

Each event is a record consisting of:

| Size  | Name | Meaning | 
| ----- |: ---:| -------:|
| 1     | Type | The type of the event, see later |
| 1     | Counter | Ever increasing counter starting at 1 ending at 0xff | 
| 1     | Size | Number of bytes in the data |
| size  | Data | The data of the event |
 

The event types are:

* 0x00: Power up, no data.
* 0x01: Wiegand: An RFID was scanned or a key was pressed on the keypad.
* 0x03: GPIO: Current state of both outputs and inputs.
* 0x04: Control token: A new 32 bit token for sending output signals
* 0x05: Log message: Informational text.


## Set output: 0x05

Sent by the controller to change an output of the device.

The payload is encrypted as described in Encrypted Payload

If a device gets a set output command with a bad control token, then it will issue a new token, but ignore the output command.

If a device gets a valid token, then it will perform the command and issue a new token.

The plain text payload, CRC contains:

| Size | Name | Meaning | 
| ---- |: ---:| -------:|
| 1    | last event | The last event seen by the controller, similar to a poll message |
| 4    | Control Token | The current control token issued by the device |
| 1    | Set state | The output state to set |
| 1    | Timeout | The number of seconds to wait before resetting outputs |
| 1    | Reset state | The state to set after the timeout |


This mechanism allows the state to be reset by the device after a certain timeout in case the controller fails.

The reply to a set output message is the same as for a poll




