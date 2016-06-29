package com.example.kav.bluetoothexample.Service;

import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;

import com.example.kav.bluetoothexample.Activity.MainActivity;
import com.example.kav.bluetoothexample.R;
import com.example.kav.bluetoothexample.UnlockService.IUnlock;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by kav on 16/06/29.
 */
public class ScanThreadJellyBean extends Thread {
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothAdapter.LeScanCallback scanCallBack = null;
    private int distanceHigh = -52;
    private final UUID SYSTEM_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private final UUID MODULE_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private final UUID BASE_UUID = UUID.fromString("00000000-0000-1000-8000-00805F9B34FB");
    private UUID[] SYSTEM_UUIDS = {SYSTEM_UUID, MODULE_UUID, BASE_UUID};
    private Map<String, List<Integer>> resultsRssiMap = new HashMap<>();
    private int rssiSwimWindow = 0;
    private int delayScan = 5000;
    private Context context = null;
    private boolean isFirst = true;
    private ScanThreadJellyBean currentThread = null;
    private String TAG = "SCAN THREAD";
    private IUnlock unlockClient = null;
    private ScheduledThreadPoolExecutor stopThread;


    public ScanThreadJellyBean(Context context, IUnlock unlockClient) {
        this.unlockClient = unlockClient;
        this.context = context;
    }

    @Override
    public void run() {
        Log.e(TAG, "START SCAN");
        unlockClient.stopChecking(context);
        if (stopThread == null || stopThread.isShutdown()) {
            stopThread = new ScheduledThreadPoolExecutor(1);
        } else {
            return;
        }
        stopThread.schedule(new Runnable() {
            public void run() {
                onDestroy();
            }
        }, delayScan, TimeUnit.MILLISECONDS);

        scanCallBack = new BluetoothAdapter.LeScanCallback() {
            List<Integer> swimWindow = null;

            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                List<ParcelUuid> uuids = BluetoothScanRecord.parseFromBytes(scanRecord).getServiceUuids();
                Log.e("UUID", uuids.get(0).getUuid().toString());
                Intent intent = new Intent(MainActivity.INTENT_FILTER_RSSI);
                if (!resultsRssiMap.containsKey(device.getAddress())) {
                    swimWindow = new ArrayList<>();
                    resultsRssiMap.put(device.getAddress(), swimWindow);
                } else {
                    swimWindow = resultsRssiMap.get(device.getAddress());
                }
                swimWindow.add(rssi);
                if (swimWindow.size() == 5) {
                    swimWindow.remove(0);
                }
                rssiSwimWindow = 0;
                for (Integer currentRSSI : swimWindow) {
                    rssiSwimWindow += currentRSSI;
                }
                rssiSwimWindow /= swimWindow.size();

                if (rssiSwimWindow > distanceHigh) {
                    Log.e(TAG, "Device found!!!");
                    sendNotification(device);
                    synchronized (this) {
                        if (isFirst) {
                            new ConnectThread(context, device).start();
                            isFirst = false;
                        }
                    }
                    onDestroy();
                }
                intent.putExtra(MainActivity.ADDRESS_INTENT, device.getAddress());
                intent.putExtra(MainActivity.NAME_INTENT, device.getName());
                intent.putExtra(MainActivity.RSSI_INTENT, (Integer) rssiSwimWindow);
                intent.putExtra(MainActivity.POWER_COUNT, -100);
                context.sendBroadcast(intent);

            }
        };
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.startLeScan(scanCallBack);
    }


    private void sendNotification(BluetoothDevice device) {
        String message = "Имя:" + device.getName() +
                "\nАдресс:" + device.getAddress() +
                "\nUUID:" + device.getUuids()[0].toString();
        NotificationCompat.Builder notificationCompatBuilder = new NotificationCompat.Builder(context);
        notificationCompatBuilder.setSmallIcon(R.mipmap.ic_launcher);
        notificationCompatBuilder.setContentTitle("Найдено устройство");
        notificationCompatBuilder.setContentText(message);
        notificationCompatBuilder.setDefaults(Notification.DEFAULT_SOUND);
        notificationCompatBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        NotificationManagerCompat.from(context).notify("Tag", MainActivity.notificationID, notificationCompatBuilder.build());
        MainActivity.notificationID++;
    }

    public void onDestroy() {
        if (bluetoothAdapter != null) {
            bluetoothAdapter.stopLeScan(scanCallBack);
            bluetoothAdapter = null;
        }
        if (currentThread != null && !currentThread.isInterrupted()) {
            currentThread.interrupt();
            currentThread = null;
        }
        unlockClient.startChecking(context);
        Log.e(TAG, "DESTROYED!");
    }


    public interface BluetoothScannerResult {
        void bluetoothDeviceFound(final BluetoothDevice device);
    }

    private static class BluetoothScanRecord {
        // The following data type values are assigned by Bluetooth SIG.
        // For more details refer to Bluetooth 4.1 specification, Volume 3, Part C, Section 18.
        private static final int DATA_TYPE_FLAGS = 0x01;
        private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL = 0x02;
        private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE = 0x03;
        private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL = 0x04;
        private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE = 0x05;
        private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL = 0x06;
        private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE = 0x07;
        private static final int DATA_TYPE_LOCAL_NAME_SHORT = 0x08;
        private static final int DATA_TYPE_LOCAL_NAME_COMPLETE = 0x09;
        private static final int DATA_TYPE_TX_POWER_LEVEL = 0x0A;
        private static final int DATA_TYPE_SERVICE_DATA = 0x16;
        private static final int DATA_TYPE_MANUFACTURER_SPECIFIC_DATA = 0xFF;

        // Flags of the advertising data.
        private final int mAdvertiseFlags;

        @Nullable
        private final List<ParcelUuid> mServiceUuids;

        private final SparseArray<byte[]> mManufacturerSpecificData;

        private final Map<ParcelUuid, byte[]> mServiceData;

        // Transmission power level(in dB).
        private final int mTxPowerLevel;

        // Local name of the Bluetooth LE device.
        private final String mDeviceName;

        // Raw bytes of scan record.
        private final byte[] mBytes;

        /**
         * Returns the advertising flags indicating the discoverable mode and capability of the device.
         * Returns -1 if the flag field is not set.
         */
        public int getAdvertiseFlags() {
            return mAdvertiseFlags;
        }

        /**
         * Returns a list of service UUIDs within the advertisement that are used to identify the
         * bluetooth GATT services.
         */
        public List<ParcelUuid> getServiceUuids() {
            return mServiceUuids;
        }

        public boolean hasServiceUuid(ParcelUuid serviceUuid) {
            return mServiceUuids != null && mServiceUuids.contains(serviceUuid);
        }

        /**
         * Returns a sparse array of manufacturer identifier and its corresponding manufacturer specific
         * data.
         */
        public SparseArray<byte[]> getManufacturerSpecificData() {
            return mManufacturerSpecificData;
        }

        /**
         * Returns the manufacturer specific data associated with the manufacturer id. Returns
         * {@code null} if the {@code manufacturerId} is not found.
         */
        @Nullable
        public byte[] getManufacturerSpecificData(int manufacturerId) {
            return mManufacturerSpecificData.get(manufacturerId);
        }

        /**
         * Returns a map of service UUID and its corresponding service data.
         */
        public Map<ParcelUuid, byte[]> getServiceData() {
            return mServiceData;
        }

        /**
         * Returns the service data byte array associated with the {@code serviceUuid}. Returns
         * {@code null} if the {@code serviceDataUuid} is not found.
         */
        @Nullable
        public byte[] getServiceData(ParcelUuid serviceDataUuid) {
            if (serviceDataUuid == null) {
                return null;
            }
            return mServiceData.get(serviceDataUuid);
        }

        /**
         * Returns the transmission power level of the packet in dBm. Returns {@link Integer#MIN_VALUE}
         * if the field is not setLevel. This value can be used to calculate the path loss of a received
         * packet using the following equation:
         * <p/>
         * <code>pathloss = txPowerLevel - rssi</code>
         */
        public int getTxPowerLevel() {
            return mTxPowerLevel;
        }

        /**
         * Returns the local name of the BLE device. The is a UTF-8 encoded string.
         */
        @Nullable
        public String getDeviceName() {
            return mDeviceName;
        }

        /**
         * Returns raw bytes of scan record.
         */
        public byte[] getBytes() {
            return mBytes;
        }

        private BluetoothScanRecord(@Nullable List<ParcelUuid> serviceUuids,
                                    SparseArray<byte[]> manufacturerData,
                                    Map<ParcelUuid, byte[]> serviceData,
                                    int advertiseFlags, int txPowerLevel,
                                    String localName, byte[] bytes) {
            mServiceUuids = serviceUuids;
            mManufacturerSpecificData = manufacturerData;
            mServiceData = serviceData;
            mDeviceName = localName;
            mAdvertiseFlags = advertiseFlags;
            mTxPowerLevel = txPowerLevel;
            mBytes = bytes;
        }

        /**
         * Parse scan record bytes to {@link BluetoothScanRecord}.
         * <p/>
         * The format is defined in Bluetooth 4.1 specification, Volume 3, Part C, Section 11 and 18.
         * <p/>
         * All numerical multi-byte entities and values shall use little-endian <strong>byte</strong>
         * order.
         *
         * @param scanRecord The scan record of Bluetooth LE advertisement and/or scan response.
         */
  /* package */
        static BluetoothScanRecord parseFromBytes(byte[] scanRecord) {
            if (scanRecord == null) {
                return null;
            }

            int currentPos = 0;
            int advertiseFlag = -1;
            List<ParcelUuid> serviceUuids = new ArrayList<>();
            String localName = null;
            int txPowerLevel = Integer.MIN_VALUE;

            SparseArray<byte[]> manufacturerData = new SparseArray<>();
            Map<ParcelUuid, byte[]> serviceData = new ArrayMap<>();

            try {
                while (currentPos < scanRecord.length) {
                    // length is unsigned int.
                    int length = scanRecord[currentPos++] & 0xFF;
                    if (length == 0) {
                        break;
                    }
                    // Note the length includes the length of the field type itself.
                    int dataLength = length - 1;
                    // fieldType is unsigned int.
                    int fieldType = scanRecord[currentPos++] & 0xFF;
                    switch (fieldType) {
                        case DATA_TYPE_FLAGS:
                            advertiseFlag = scanRecord[currentPos] & 0xFF;
                            break;
                        case DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL:
                        case DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE:
                            parseServiceUuid(scanRecord, currentPos,
                                    dataLength, BluetoothUuid.UUID_BYTES_16_BIT, serviceUuids);
                            break;
                        case DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL:
                        case DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE:
                            parseServiceUuid(scanRecord, currentPos, dataLength,
                                    BluetoothUuid.UUID_BYTES_32_BIT, serviceUuids);
                            break;
                        case DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL:
                        case DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE:
                            parseServiceUuid(scanRecord, currentPos, dataLength,
                                    BluetoothUuid.UUID_BYTES_128_BIT, serviceUuids);
                            break;
                        case DATA_TYPE_LOCAL_NAME_SHORT:
                        case DATA_TYPE_LOCAL_NAME_COMPLETE:
                            localName = new String(
                                    extractBytes(scanRecord, currentPos, dataLength));
                            break;
                        case DATA_TYPE_TX_POWER_LEVEL:
                            txPowerLevel = scanRecord[currentPos];
                            break;
                        case DATA_TYPE_SERVICE_DATA:
                            // The first two bytes of the service data are service data UUID in little
                            // endian. The rest bytes are service data.
                            int serviceUuidLength = BluetoothUuid.UUID_BYTES_16_BIT;
                            byte[] serviceDataUuidBytes = extractBytes(scanRecord, currentPos,
                                    serviceUuidLength);
                            ParcelUuid serviceDataUuid = BluetoothUuid.parseUuidFrom(
                                    serviceDataUuidBytes);
                            byte[] serviceDataArray = extractBytes(scanRecord,
                                    currentPos + serviceUuidLength, dataLength - serviceUuidLength);
                            serviceData.put(serviceDataUuid, serviceDataArray);
                            break;
                        case DATA_TYPE_MANUFACTURER_SPECIFIC_DATA:
                            // The first two bytes of the manufacturer specific data are
                            // manufacturer ids in little endian.
                            int manufacturerId = ((scanRecord[currentPos + 1] & 0xFF) << 8) +
                                    (scanRecord[currentPos] & 0xFF);
                            byte[] manufacturerDataBytes = extractBytes(scanRecord, currentPos + 2,
                                    dataLength - 2);
                            manufacturerData.put(manufacturerId, manufacturerDataBytes);
                            break;
                        default:
                            // Just ignore, we don't handle such data type.
                            break;
                    }
                    currentPos += dataLength;
                }

                if (serviceUuids.isEmpty()) {
                    serviceUuids = null;
                }
                return new BluetoothScanRecord(serviceUuids, manufacturerData, serviceData,
                        advertiseFlag, txPowerLevel, localName, scanRecord);
            } catch (Exception e) {
                e.printStackTrace();
                // As the record is invalid, ignore all the parsed results for this packet
                // and return an empty record with raw scanRecord bytes in results
                return new BluetoothScanRecord(null, null, null, -1, Integer.MIN_VALUE, null, scanRecord);
            }
        }

        // Parse service UUIDs.
        private static int parseServiceUuid(byte[] scanRecord, int currentPos, int dataLength, int uuidLength, List<ParcelUuid> serviceUuids) {
            while (dataLength > 0) {
                byte[] uuidBytes = extractBytes(scanRecord, currentPos,
                        uuidLength);
                serviceUuids.add(BluetoothUuid.parseUuidFrom(uuidBytes));
                dataLength -= uuidLength;
                currentPos += uuidLength;
            }
            return currentPos;
        }

        // Helper method to extract bytes from byte array.
        private static byte[] extractBytes(byte[] scanRecord, int start, int length) {
            byte[] bytes = new byte[length];
            System.arraycopy(scanRecord, start, bytes, 0, length);
            return bytes;
        }
    }


    private static final class BluetoothUuid {
        static final ParcelUuid BASE_UUID = ParcelUuid.fromString("00000000-0000-1000-8000-00805F9B34FB");
        /**
         * Length of bytes for 16 bit UUID
         */
        static final int UUID_BYTES_16_BIT = 2;
        /**
         * Length of bytes for 32 bit UUID
         */
        static final int UUID_BYTES_32_BIT = 4;
        /**
         * Length of bytes for 128 bit UUID
         */
        static final int UUID_BYTES_128_BIT = 16;

        /**
         * Parse UUID from bytes. The {@code uuidBytes} can represent a 16-bit, 32-bit or 128-bit UUID,
         * but the returned UUID is always in 128-bit format.
         * Note UUID is little endian in Bluetooth.
         *
         * @param uuidBytes Byte representation of uuid.
         * @return {@link ParcelUuid} parsed from bytes.
         * @throws IllegalArgumentException If the {@code uuidBytes} cannot be parsed.
         */
        static ParcelUuid parseUuidFrom(byte[] uuidBytes) {
            if (uuidBytes == null) {
                throw new IllegalArgumentException("uuidBytes cannot be null");
            }
            int length = uuidBytes.length;
            if (length != UUID_BYTES_16_BIT && length != UUID_BYTES_32_BIT &&
                    length != UUID_BYTES_128_BIT) {
                throw new IllegalArgumentException("uuidBytes length invalid - " + length);
            }

            // Construct a 128 bit UUID.
            if (length == UUID_BYTES_128_BIT) {
                ByteBuffer buf = ByteBuffer.wrap(uuidBytes).order(ByteOrder.LITTLE_ENDIAN);
                long msb = buf.getLong(8);
                long lsb = buf.getLong(0);
                return new ParcelUuid(new UUID(msb, lsb));
            }

            // For 16 bit and 32 bit UUID we need to convert them to 128 bit value.
            // 128_bit_value = uuid * 2^96 + BASE_UUID
            long shortUuid;
            if (length == UUID_BYTES_16_BIT) {
                shortUuid = uuidBytes[0] & 0xFF;
                shortUuid += (uuidBytes[1] & 0xFF) << 8;
            } else {
                shortUuid = uuidBytes[0] & 0xFF;
                shortUuid += (uuidBytes[1] & 0xFF) << 8;
                shortUuid += (uuidBytes[2] & 0xFF) << 16;
                shortUuid += (uuidBytes[3] & 0xFF) << 24;
            }
            long msb = BASE_UUID.getUuid().getMostSignificantBits() + (shortUuid << 32);
            long lsb = BASE_UUID.getUuid().getLeastSignificantBits();
            return new ParcelUuid(new UUID(msb, lsb));
        }
    }

}