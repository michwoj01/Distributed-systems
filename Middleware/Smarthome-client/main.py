import os
import re

from thrift.Thrift import TApplicationException
from thrift.protocol import TMultiplexedProtocol
from thrift.protocol.TBinaryProtocol import TBinaryProtocol
from thrift.transport import TSocket, TTransport
from thrift.transport.TTransport import TBufferedTransport

from generated import Smarthome, Stove, Fridge, Cleaner
from generated.ttypes import StoveType, DeviceType, DangerOperation, DangerException, InvalidArgumentException, \
    StoveObject

smarthomeClient = None
stoveClient = None
cleanerClient = None
fridgeClient = None

chosenDevice = None
deviceMode = DeviceType.NONE

connections = {}

pattern = re.compile("^-?[1-9][0-9]?.?[0-9]?[0-9]?$")


def connect(port: str):
    if port not in connections.keys():
        try:
            transport = TBufferedTransport(TSocket.TSocket('localhost', int(port)))
            transport.open()
        except TTransport.TTransportException:
            print("No open server on that port")
            return
        protocol = TBinaryProtocol(transport)
        connections[port] = protocol

    global smarthomeClient, stoveClient, cleanerClient, fridgeClient
    smarthomeClient = Smarthome.Client(TMultiplexedProtocol.TMultiplexedProtocol(connections[port], "Smarthome"))
    stoveClient = Stove.Client(TMultiplexedProtocol.TMultiplexedProtocol(connections[port], "Stove"))
    cleanerClient = Cleaner.Client(TMultiplexedProtocol.TMultiplexedProtocol(connections[port], "Cleaner"))
    fridgeClient = Fridge.Client(TMultiplexedProtocol.TMultiplexedProtocol(connections[port], "Fridge"))
    print(f"Successfully switched to server on port {port}")


def welcome_msg():
    print("""\nWhat would you like to do?
1. List all devices
2. Choose devices to control (2 deviceId)
3. Change server (3 port)
4. Exit (I'm bored)""")


def print_stove():
    print("{:5s} {:10s} {:10s} {:10s} {:20s}".format("ID", "TYPE", "HEAT", "PRESSURE", "MESSAGE"))
    print(stoveClient.checkState(chosenDevice).__getattribute__("message"))
    print("""
1. Heat water (1 newTemperature)
2. Create invoice
3. Exit (I'm bored)""")


def print_cleaner():
    print("{:5s} {:10s} {:10s} {:20s}".format("ID", "POWER", "BAG", "MESSAGE"))
    print(cleanerClient.checkState(chosenDevice).__getattribute__("message"))
    print("""
1. Charge the vacuum cleaner
2. Empty the garbage bag
3. Vacuum the room
4. Exit (I'm bored)""")


def print_fridge():
    print("{:5s} {:10s} {:20s}".format("ID", "COLD", "MESSAGE"))
    print(fridgeClient.checkState(chosenDevice).__getattribute__("message"))
    print("""
1. Change the temperature (1 newTemperature)
2. Exit (I'm bored)""")


def present_subsidy(stove_object: StoveObject):
    print(f"""Dear taxpayer, here are the parameters of your subsidy calculated by the Ministry of Finance based on our stove:
Stove = {StoveType._VALUES_TO_NAMES[stove_object.type]} 
Discount = {stove_object.subsidy.discount}
Limit for usage = {stove_object.subsidy.limit}
Fee payable = {stove_object.subsidy.sum}
Enjoy cheap energy under the government's plan The New Scam
""")


def show_devices():
    full_list = smarthomeClient.getAllDevices()
    print("Welcome to your smarthome! Here are available devices:\n")
    print("Stoves:")
    print("{:5s} {:10s} {:10s} {:10s} {:20s}".format("ID", "TYPE", "HEAT", "PRESSURE", "MESSAGE"))
    for stove in full_list.stoves:
        message = stove.message if stove.message is not None else ""
        print("{:<5d} {:<10s} {:<10.2f} {:<10.2f} {:<20s}".format(stove.id,
                                                                  StoveType._VALUES_TO_NAMES[stove.type],
                                                                  stove.heat, stove.pressure, message))
    print("Cleaners:")
    print("{:5s} {:10s} {:10s} {:20s}".format("ID", "POWER", "BAG", "MESSAGE"))
    for cleaner in full_list.cleaners:
        message = cleaner.message if cleaner.message is not None else ""
        print("{:<5d} {:<10.2f} {:<10.2f} {:<20s}".format(cleaner.id, cleaner.battery, cleaner.capacity, message))
    print("Fridges:")
    print("{:5s} {:10s} {:20s}".format("ID", "COLD", "MESSAGE"))
    for fridge in full_list.fridges:
        message = fridge.message if fridge.message is not None else ""
        print("{:<5d} {:<10.2f} {:<20s}".format(fridge.id, fridge.cold, message))


def deal_with_tae(invalid_arg_exc: TApplicationException):
    print(f"""Hey, somethings wrong, I can feel it. Yeah, I knew it.
I got some exception on server side. 
Would be nicer to know which one, but we are not living in fairyland.

Here it's message: {invalid_arg_exc.__getattribute__("message")}
""")


def deal_with_de(danger_exc: DangerException):
    print(f"""Hey, somethings wrong, I can feel it. Yeah, I knew it.
I got some exception on server side. And it's danger exception

Here it's type: {DangerOperation._VALUES_TO_NAMES[danger_exc.__getattribute__("whatOp")]}
Here it's message: {danger_exc.__getattribute__("message")}
""")


def deal_with_iae(invalid_arg_exc: InvalidArgumentException):
    print(f"""Bro, you must made a mistake. That args ain't fill/ 

Here it's message: {invalid_arg_exc.__getattribute__("message")}
""")


print("Welcome to smarthome, connect to server on given port:")
while True:
    choice = input()
    if not choice.isdigit():
        print("Bad argument type")
        continue
    connect(choice)
    break

os.system('cls')
while True:
    match deviceMode:
        case DeviceType.NONE:
            while True:
                welcome_msg()
                choice = input()
                os.system('cls')
                match choice:
                    case "1":
                        show_devices()
                    case value if value.startswith("2"):
                        args = choice.split(" ")
                        if len(args) < 2 or not args[1].isdigit():
                            print("Bad argument type")
                            continue
                        estimatedId = int(args[1])
                        estimatedType = smarthomeClient.getDeviceType(estimatedId)
                        if estimatedType != DeviceType.NONE:
                            deviceMode = estimatedType
                            chosenDevice = estimatedId
                            break
                        else:
                            print("There is no such device, try again")
                            continue
                    case value if value.startswith("3"):
                        args = choice.split(" ")
                        if len(args) < 2 or not args[1].isdigit():
                            print("Bad argument type")
                            continue
                        connect(args[1])
                    case "4":
                        exit(0)
                    case _:
                        continue
        case DeviceType.STOVE:
            while True:
                print_stove()
                choice = input()
                os.system('cls')
                match choice:
                    case value if value.startswith("1"):
                        args = choice.split(" ")
                        if len(args) < 2 or not pattern.match(args[1]):
                            print("Bad argument type")
                            continue
                        try:
                            stoveClient.heatWater(chosenDevice, float(args[1]))
                        except DangerException as de:
                            deal_with_de(de)
                        except InvalidArgumentException as iae:
                            deal_with_iae(iae)
                        except TApplicationException as tae:
                            deal_with_tae(tae)
                    case "2":
                        subsidy = stoveClient.createInvoice(chosenDevice)
                        present_subsidy(subsidy)
                    case "3":
                        chosenDevice = None
                        deviceMode = DeviceType.NONE
                        break
                    case _:
                        continue
        case DeviceType.CLEANER:
            while True:
                print_cleaner()
                choice = input()
                os.system('cls')
                match choice:
                    case "1":
                        cleanerClient.loadCleaner(chosenDevice)
                    case "2":
                        cleanerClient.emptyBag(chosenDevice)
                    case "3":
                        try:
                            cleanerClient.vacuumRoom(chosenDevice)
                        except DangerException as de:
                            deal_with_de(de)
                        except TApplicationException as tae:
                            deal_with_tae(tae)
                    case "4":
                        chosenDevice = None
                        deviceMode = DeviceType.NONE
                        break
                    case _:
                        continue
        case other:
            while True:
                print_fridge()
                choice = input()
                os.system('cls')
                match choice:
                    case value if value.startswith("1"):
                        args = choice.split(" ")
                        if len(args) < 2 or not pattern.match(args[1]):
                            print("Bad argument type")
                            continue
                        try:
                            fridgeClient.changeTemperature(chosenDevice, float(args[1]))
                        except InvalidArgumentException as iae:
                            deal_with_iae(iae)
                        except TApplicationException as tae:
                            deal_with_tae(tae)
                    case "2":
                        chosenDevice = None
                        deviceMode = DeviceType.NONE
                        break
                    case _:
                        continue
