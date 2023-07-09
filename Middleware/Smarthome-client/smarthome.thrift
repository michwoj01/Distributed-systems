enum DangerOperation {
  OVERHEAT = 1,
  EXHAUSTION = 2,
  OVERFLOW = 3,
}

enum DeviceType {
  NONE = 0,
  STOVE = 1,
  CLEANER = 2,
  FRIDGE = 3
}

exception DangerException {
  1: DangerOperation whatOp,
  2: string message
}

exception InvalidArgumentException {
  1: string message
}

enum StoveType {
  TILED = 1,
  GAS = 2,
  PUMP = 3,
}

struct Subsidy {
  1: double discount,
  2: i32 limit,
  3: double sum
}

struct StoveObject  {
  1: i32 id,
  2: StoveType type,
  3: double heat,
  4: double pressure,
  5: optional string message,
  6: optional Subsidy subsidy
}

struct DeviceInfo {
  1: string message
}

service Device {
  DeviceInfo checkState(1:i32 deviceId)
}

service Stove extends Device {
   void heatWater(1:i32 stoveId, 2: double temp) throws (1:DangerException ex, 2:InvalidArgumentException ex2),
   StoveObject createInvoice(1: i32 stoveId)
}

struct CleanerObject {
  1: i32 id,
  2: double battery,
  3: double capacity,
  4: optional string message
}

service Cleaner extends Device {
  void loadCleaner(1:i32 cleanerId),
  void vacuumRoom(1:i32 cleanerId) throws (1:DangerException ex),
  void emptyBag(1:i32 cleanerId)
}

struct FridgeObject {
  1: i32 id,
  2: double cold,
  3: optional string message
}

service Fridge extends Device {
   void changeTemperature(1:i32 fridgeId, 2:double temp) throws (1:InvalidArgumentException ex)
}

struct DeviceSummary {
   1: list<StoveObject> stoves,
   2: list<CleanerObject> cleaners,
   3: list<FridgeObject> fridges,
}

service Smarthome {
   DeviceSummary getAllDevices()
   DeviceType getDeviceType(1:i32 deviceId)
}