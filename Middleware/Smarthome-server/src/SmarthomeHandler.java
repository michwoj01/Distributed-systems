import org.apache.thrift.TException;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SmarthomeHandler implements Smarthome.Iface {
    private int genId = 0;
    public static final Map<Integer, StoveObject> stoves = new HashMap<>();
    public static final Map<Integer, CleanerObject> cleaners = new HashMap<>();
    public static final Map<Integer, FridgeObject> fridges = new HashMap<>();

    public void initialize() {
        Random random = ThriftServer.rand;
        int NODES = 10;
        for (int i = 0; i < NODES; i++) {
            int type = random.nextInt(3);
            switch (type) {
                case 0 -> {
                    StoveObject stove = new StoveObject(
                            genId++,
                            StoveType.findByValue(random.nextInt(1, 4)),
                            (double) Math.round(random.nextDouble(25, 83.5) * 100) / 100,
                            (double) Math.round(random.nextDouble(0.5, 3.5) * 100) / 100
                    );
                    stoves.put(genId - 1, stove);
                }
                case 1 -> {
                    CleanerObject cleaner = new CleanerObject(
                            genId++,
                            (double) Math.round(random.nextDouble(10, 90) * 100) / 100,
                            (double) Math.round(random.nextDouble(25, 75) * 100) / 100
                    );
                    cleaners.put(genId - 1, cleaner);
                }
                default -> {
                    FridgeObject fridge = new FridgeObject(
                            genId++,
                            (double) Math.round(random.nextDouble(-10, 15) * 100) / 100
                    );
                    fridges.put(genId - 1, fridge);
                }
            }
        }
    }

    public static void cleanup() {
        stoves.clear();
        cleaners.clear();
        fridges.clear();
    }

    @Override
    public DeviceSummary getAllDevices() throws TException {
        return new DeviceSummary(stoves.values().stream().toList(), cleaners.values().stream().toList(), fridges.values().stream().toList());
    }

    @Override
    public DeviceType getDeviceType(int deviceId) throws TException {
        if (stoves.containsKey(deviceId)) {
            return DeviceType.STOVE;
        } else if (cleaners.containsKey(deviceId)) {
            return DeviceType.CLEANER;
        } else if (fridges.containsKey(deviceId)) {
            return DeviceType.FRIDGE;
        } else {
            return DeviceType.NONE;
        }
    }
}
