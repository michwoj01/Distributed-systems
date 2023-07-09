public class CleanerHandler implements Cleaner.Iface {
    @Override
    public void loadCleaner(int cleanerId) {
        CleanerObject cleaner = SmarthomeHandler.cleaners.get(cleanerId);
        double load = ThriftServer.rand.nextDouble(50);
        double oldBattery = cleaner.battery;
        cleaner.battery = Math.min(oldBattery + load, 100);
        if (cleaner.getBattery() == 100) {
            cleaner.setMessage("Full load of battery!");
        }
        SmarthomeHandler.cleaners.put(cleanerId, cleaner);
    }


    @Override
    public void vacuumRoom(int cleanerId) throws DangerException {
        CleanerObject cleaner = SmarthomeHandler.cleaners.get(cleanerId);
        double dirt = ThriftServer.rand.nextDouble(50);
        double energy = ThriftServer.rand.nextDouble(50);
        if (cleaner.getCapacity() + dirt > 100) {
            throw new DangerException(DangerOperation.OVERFLOW, "Not enough space for vacuuming, empty the bag!");
        } else if (cleaner.getBattery() - energy < 0) {
            throw new DangerException(DangerOperation.EXHAUSTION, "Not enough battery, load the cleaner!");
        } else {
            cleaner.capacity += dirt;
            cleaner.battery -= energy;
            if (cleaner.getCapacity() > 90 && cleaner.getBattery() < 10) {
                cleaner.setMessage("Less than 10% space and 10% battery left.");
            } else if (cleaner.getCapacity() > 90) {
                cleaner.setMessage("Less than 10% space left.");
            } else if (cleaner.getBattery() < 10) {
                cleaner.setMessage("Less than 10% battery left.");
            } else {
                cleaner.setMessage("");
            }
            SmarthomeHandler.cleaners.put(cleanerId, cleaner);
        }
    }

    @Override
    public void emptyBag(int cleanerId) {
        CleanerObject cleaner = SmarthomeHandler.cleaners.get(cleanerId);
        double energy = ThriftServer.rand.nextDouble(10);
        cleaner.capacity = 0;
        cleaner.battery -= energy;
        cleaner.setMessage("Bag has been emptied!");
        SmarthomeHandler.cleaners.put(cleanerId, cleaner);
    }

    @Override
    public DeviceInfo checkState(int cleanerId) {
        return toString(SmarthomeHandler.cleaners.get(cleanerId));
    }


    private DeviceInfo toString(CleanerObject cleanerObject) {
        String message = cleanerObject.message == null ? "" : cleanerObject.message;
        return new DeviceInfo(String.format("%-5d %-10.2f %-10.2f %-20s",
                cleanerObject.id, cleanerObject.battery, cleanerObject.capacity, message));
    }
}
