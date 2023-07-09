import java.util.List;
import java.util.stream.DoubleStream;

public class CalculatorHandler implements Calculator.Iface {

    @Override
    public double countMedian(Person person) {
        List<Double> list = person.taxes;
        if (list == null || list.isEmpty()) {
            return Double.NaN;
        }
        DoubleStream stream = list.stream().sorted().mapToDouble(Double::doubleValue);
        return list.size() % 2 == 0 ?
                stream.skip(list.size() / 2 - 1).limit(2).average().getAsDouble() :
                stream.skip(list.size() / 2).findFirst().getAsDouble();
    }

}

