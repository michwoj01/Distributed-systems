import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.stream.DoubleStream;

public class CalculatorImpl extends CalculatorGrpc.CalculatorImplBase {
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
}
