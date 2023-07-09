import logging

from generated.optionals_pb2_grpc import *


def run():
    with grpc.insecure_channel('127.0.0.5:50051') as channel:
        print("Connected to server")
        stub = CalculatorStub(channel)
        while True:
            choice = input()
            match choice:
                case "basic":
                    response = stub.CountMedian(Person(name="Michal"))
                    print(response)
                case "id":
                    response = stub.CountMedian(Person(name="Michal", id=2))
                    print(response)
                case "email":
                    response = stub.CountMedian(Person(name="Michal", id=2, email="michwoj01@gmail.com"))
                    print(response)

                case "invoice":
                    response = stub.CountMedian(Person(name="Michal", id=2, email="michwoj01@gmail.com",
                                                       income=Person.Income(taxes=[45.6, 23.4, 23.5])))
                    print(response)


if __name__ == '__main__':
    logging.basicConfig()
    run()
