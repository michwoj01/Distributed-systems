import io.grpc.stub.StreamObserver;
import service.*;

import java.util.List;
import java.util.stream.DoubleStream;

public class ServiceImpl extends ServiceGrpc.ServiceImplBase {
    @Override
    public void add(ArithmeticOpArguments request, StreamObserver<ArithmeticOpResult> responseObserver) {
        System.out.println("AddRequest (" + request.getArg1() + ", " + request.getArg2() + ")");
        int val = request.getArg1() + request.getArg2();
        sendResult(val, responseObserver);
    }

    @Override
    public void subtract(ArithmeticOpArguments request, StreamObserver<ArithmeticOpResult> responseObserver) {
        System.out.println("SubtractRequest (" + request.getArg1() + ", " + request.getArg2() + ")");
        int val = request.getArg1() - request.getArg2();
        sendResult(val, responseObserver);
    }

    @Override
    public void multiply(ArithmeticOpArguments request, StreamObserver<ArithmeticOpResult> responseObserver) {
        System.out.println("MultiplyRequest (" + request.getArg1() + ", " + request.getArg2() + ")");
        int val = request.getArg1() * request.getArg2();
        sendResult(val, responseObserver);
    }

    @Override
    public void countMedian(Person request, StreamObserver<Median> responseObserver) {
        List<Double> list = request.getIncome().getTaxesList();
        double val;
        if (list.isEmpty()) {
            val = Double.NaN;
        } else {
            DoubleStream stream = list.stream().sorted().mapToDouble(Double::doubleValue);
            val = list.size() % 2 == 0 ?
                    stream.skip(list.size() / 2 - 1).limit(2).average().getAsDouble() :
                    stream.skip(list.size() / 2).findFirst().getAsDouble();
        }
        Median result = Median.newBuilder().setMed(val).build();
        responseObserver.onNext(result);
        responseObserver.onCompleted();
    }

    private void sendResult(int value, StreamObserver<ArithmeticOpResult> observer) {
        ArithmeticOpResult result = ArithmeticOpResult.newBuilder().setRes(value).build();
        observer.onNext(result);
        observer.onCompleted();
    }
}
