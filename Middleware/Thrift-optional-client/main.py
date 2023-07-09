import re

from thrift.protocol import TMultiplexedProtocol
from thrift.protocol.TBinaryProtocol import TBinaryProtocol
from thrift.transport import TSocket, TTransport
from thrift.transport.TTransport import TBufferedTransport

from generated.calculator import *
from generated.calculator.ttypes import Person

pattern = re.compile("^-?[1-9][0-9]?.?[0-9]?[0-9]?$")

try:
    transport = TBufferedTransport(TSocket.TSocket('localhost', 9090))
    transport.open()
    protocol = TBinaryProtocol(transport)
    handler = Calculator.Client(TMultiplexedProtocol.TMultiplexedProtocol(protocol, "Calculator"))
    while True:
        choice = input()
        match choice:
            case "basic":
                response = handler.countMedian(Person(name="Michal"))
                print(response)
            case "id":
                response = handler.countMedian(Person(name="Michal", id=2))
                print(response)
            case "email":
                response = handler.countMedian(Person(name="Michal", id=2, email="michwoj01@gmail.com"))
                print(response)

            case "invoice":
                response = handler.countMedian(
                    Person(name="Michal", id=2, email="michwoj01@gmail.com", taxes=[23.6, 28.6, 29.5]))
                print(response)

except TTransport.TTransportException:
    print("No open server on that port")
