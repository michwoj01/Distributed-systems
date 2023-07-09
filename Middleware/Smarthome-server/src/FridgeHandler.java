public class FridgeHandler implements Fridge.Iface {
    @Override
    public void changeTemperature(int fridgeId, double temp) throws InvalidArgumentException {
        if (temp < -10.0 || temp > 20.0) {
            throw new InvalidArgumentException("Bad temperature for fridge, try something between -10 and 20");
        }
        FridgeObject fridge = SmarthomeHandler.fridges.get(fridgeId);
        double oldTemp = fridge.getCold();
        if (oldTemp < temp) {
            fridge.setMessage("Fridge increasing temperature");
        } else {
            fridge.setMessage("Fridge decreasing temperature");
        }
        fridge.setCold(temp);
        SmarthomeHandler.fridges.put(fridgeId, fridge);
    }

    @Override
    public DeviceInfo checkState(int fridgeId) {
        return toString(SmarthomeHandler.fridges.get(fridgeId));
    }

    private DeviceInfo toString(FridgeObject fridgeObject) {
        String message = fridgeObject.message == null ? "" : fridgeObject.message;
        return new DeviceInfo(String.format("%-5d %-10.2f %-20s", fridgeObject.id, fridgeObject.cold, message));
    }
}
