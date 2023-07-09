public class StoveHandler implements Stove.Iface {
    @Override
    public void heatWater(int stoveId, double temp) throws InvalidArgumentException, DangerException {
        if (temp < 0) {
            throw new InvalidArgumentException("Stove cannot heat water to negative temperature");
        }
        StoveObject stove = SmarthomeHandler.stoves.get(stoveId);
        double oldHeat = stove.getHeat();
        double pressureChange = ThriftServer.rand.nextDouble(0.5, 1.5);
        if (oldHeat < temp) {
            if (stove.getPressure() + pressureChange > 5.0) {
                throw new DangerException(DangerOperation.OVERHEAT, "Stove pressure above limit, lower temperature");
            } else if (stove.getPressure() + pressureChange > 4.0) {
                stove.setMessage("Pressure above 4.0, be aware of overheating!");
            }
            stove.pressure += pressureChange;
        } else if (stove.getPressure() - pressureChange > 0.5) {
            stove.pressure -= pressureChange;
            if (stove.getPressure() < 4) {
                stove.setMessage("");
            }
        }
        stove.setHeat(temp);
        SmarthomeHandler.stoves.put(stoveId, stove);
    }

    @Override
    public StoveObject createInvoice(int stoveId) {
        StoveObject stove = SmarthomeHandler.stoves.get(stoveId);
        Subsidy subsidy = new Subsidy();
        switch (stove.type) {
            case GAS -> {
                subsidy.setDiscount(40);
                subsidy.setLimit(10000);
                int use = ThriftServer.rand.nextInt(3000, 8000);
                double price = ThriftServer.rand.nextDouble(2.0, 3.0);
                double sum = use * price * (100 - subsidy.discount) / 100.0;
                sum = Math.round(sum * 100);
                subsidy.setSum(sum / 100);
            }
            case PUMP -> {
                subsidy.setDiscount(20);
                subsidy.setLimit(8000);
                int use = ThriftServer.rand.nextInt(3000, 8000);
                double price = ThriftServer.rand.nextDouble(1.0, 2.0);
                double sum = use * price * (100 - subsidy.discount) / 100.0;
                sum = Math.round(sum * 100);
                subsidy.setSum(sum / 100);
            }
            case TILED -> {
                subsidy.setDiscount(10);
                subsidy.setLimit(5000);
                int use = ThriftServer.rand.nextInt(3000, 8000);
                double price = ThriftServer.rand.nextDouble(6.0, 10.0);
                double sum = use * price * (100 - subsidy.discount) / 100.0;
                sum = Math.round(sum * 100);
                subsidy.setSum(sum / 100);
            }
        }
        stove.setSubsidy(subsidy);
        SmarthomeHandler.stoves.put(stoveId, stove);
        return stove;
    }

    @Override
    public DeviceInfo checkState(int stoveId) {
        return toString(SmarthomeHandler.stoves.get(stoveId));
    }

    private DeviceInfo toString(StoveObject stoveObject) {
        String message = stoveObject.message == null ? "" : stoveObject.message;
        return new DeviceInfo(String.format("%-5d %-10s %-10.2f %-10.2f %-20s", stoveObject.id,
                stoveObject.type.toString(), stoveObject.heat, stoveObject.pressure, message));
    }
}
