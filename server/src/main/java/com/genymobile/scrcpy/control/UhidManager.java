package com.genymobile.scrcpy.control;

import com.genymobile.scrcpy.AndroidVersions;
import com.genymobile.scrcpy.display.DisplayInfo;
import com.genymobile.scrcpy.util.Ln;
import com.genymobile.scrcpy.util.StringUtils;
import com.genymobile.scrcpy.wrappers.ServiceManager;

import android.os.Build;
import android.os.HandlerThread;
import android.os.MessageQueue;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.ArrayMap;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public final class UhidManager {

    private static final int UHID_OUTPUT = 6;
    private static final int UHID_CREATE2 = 11;
    private static final int UHID_INPUT2 = 12;

    private static final short BUS_VIRTUAL = 0x06;

    private static final int SIZE_OF_UHID_EVENT = 4380;

    private static final String INPUT_PORT = "scrcpy:" + Os.getpid();

    private final String displayUniqueId;

    private final ArrayMap<Integer, FileDescriptor> fds = new ArrayMap<>();
    private final ByteBuffer buffer = ByteBuffer.allocate(SIZE_OF_UHID_EVENT).order(ByteOrder.nativeOrder());

    private final DeviceMessageSender sender;
    private final MessageQueue queue;

    public UhidManager(DeviceMessageSender sender, String displayUniqueId) {
        this.sender = sender;
        this.displayUniqueId = displayUniqueId != null ? displayUniqueId : findScrcpyVirtualDisplayUniqueId();
        if (Build.VERSION.SDK_INT >= AndroidVersions.API_23_ANDROID_6_0) {
            HandlerThread thread = new HandlerThread("UHidManager");
            thread.start();
            queue = thread.getLooper().getQueue();
        } else {
            queue = null;
        }
    }

    private static String findScrcpyVirtualDisplayUniqueId() {
        if (Build.VERSION.SDK_INT < AndroidVersions.API_33_ANDROID_13) {
            return null;
        }

        try {
            int[] displayIds = ServiceManager.getDisplayManager().getDisplayIds();
            for (int displayId : displayIds) {
                if (displayId == 0) {
                    continue;
                }
                DisplayInfo displayInfo = ServiceManager.getDisplayManager().getDisplayInfo(displayId);
                if (displayInfo == null) {
                    continue;
                }
                String uniqueId = displayInfo.getUniqueId();
                if (uniqueId != null && uniqueId.contains(",scrcpy,")) {
                    return uniqueId;
                }
            }
        } catch (RuntimeException e) {
            Ln.w("Could not find scrcpy virtual display", e);
        }

        return null;
    }

    public void open(int id, int vendorId, int productId, String name, byte[] reportDesc) throws IOException {
        try {
            FileDescriptor fd = Os.open("/dev/uhid", OsConstants.O_RDWR, 0);
            try {
                boolean firstDevice = fds.isEmpty();

                FileDescriptor old = fds.put(id, fd);
                if (old != null) {
                    Ln.w("Duplicate UHID id: " + id);
                    close(old);
                }

                String phys = mustUseInputPort() ? INPUT_PORT : null;
                byte[] req = buildUhidCreate2Req(vendorId, productId, name, reportDesc, phys);
                Os.write(fd, req, 0, req.length);

                if (firstDevice) {
                    addUniqueIdAssociation();
                }
                registerUhidListener(id, fd);
            } catch (Exception e) {
                close(fd);
                throw e;
            }
        } catch (ErrnoException e) {
            throw new IOException(e);
        }
    }

    private void registerUhidListener(int id, FileDescriptor fd) {
        if (Build.VERSION.SDK_INT >= AndroidVersions.API_23_ANDROID_6_0) {
            queue.addOnFileDescriptorEventListener(fd, MessageQueue.OnFileDescriptorEventListener.EVENT_INPUT, (fd2, events) -> {
                try {
                    buffer.clear();
                    int r = Os.read(fd2, buffer);
                    buffer.flip();
                    if (r > 0) {
                        int type = buffer.getInt();
                        if (type == UHID_OUTPUT) {
                            byte[] data = extractHidOutputData(buffer);
                            if (data != null) {
                                DeviceMessage msg = DeviceMessage.createUhidOutput(id, data);
                                sender.send(msg);
                            }
                        }
                    }
                } catch (ErrnoException | InterruptedIOException e) {
                    Ln.e("Failed to read UHID output", e);
                    return 0;
                }
                return events;
            });
        }
    }

    private void unregisterUhidListener(FileDescriptor fd) {
        if (Build.VERSION.SDK_INT >= AndroidVersions.API_23_ANDROID_6_0) {
            queue.removeOnFileDescriptorEventListener(fd);
        }
    }

    private static byte[] extractHidOutputData(ByteBuffer buffer) {
        if (buffer.remaining() < 4099) {
            Ln.w("Incomplete HID output");
            return null;
        }
        int size = buffer.getShort(buffer.position() + 4096) & 0xFFFF;
        if (size > 4096) {
            Ln.w("Incorrect HID output size: " + size);
            return null;
        }
        byte[] data = new byte[size];
        buffer.get(data);
        return data;
    }

    public void writeInput(int id, byte[] data) throws IOException {
        FileDescriptor fd = fds.get(id);
        if (fd == null) {
            Ln.w("Unknown UHID id: " + id);
            return;
        }

        try {
            byte[] req = buildUhidInput2Req(data);
            Os.write(fd, req, 0, req.length);
        } catch (ErrnoException e) {
            throw new IOException(e);
        }
    }

    private static byte[] buildUhidCreate2Req(int vendorId, int productId, String name, byte[] reportDesc, String phys) {
        ByteBuffer buf = ByteBuffer.allocate(280 + reportDesc.length).order(ByteOrder.nativeOrder());
        buf.putInt(UHID_CREATE2);

        String actualName = name.isEmpty() ? "scrcpy" : name;
        byte[] nameBytes = actualName.getBytes(StandardCharsets.UTF_8);
        int nameLen = StringUtils.getUtf8TruncationIndex(nameBytes, 127);
        assert nameLen <= 127;
        buf.put(nameBytes, 0, nameLen);

        if (phys != null) {
            buf.position(4 + 128);
            byte[] physBytes = phys.getBytes(StandardCharsets.US_ASCII);
            assert physBytes.length <= 63;
            buf.put(physBytes);
        }

        buf.position(4 + 256);
        buf.putShort((short) reportDesc.length);
        buf.putShort(BUS_VIRTUAL);
        buf.putInt(vendorId);
        buf.putInt(productId);
        buf.putInt(0);
        buf.putInt(0);
        buf.put(reportDesc);
        return buf.array();
    }

    private static byte[] buildUhidInput2Req(byte[] data) {
        ByteBuffer buf = ByteBuffer.allocate(6 + data.length).order(ByteOrder.nativeOrder());
        buf.putInt(UHID_INPUT2);
        buf.putShort((short) data.length);
        buf.put(data);
        return buf.array();
    }

    public void close(int id) {
        FileDescriptor fd = fds.remove(id);
        if (fd != null) {
            unregisterUhidListener(fd);
            close(fd);

            if (fds.isEmpty()) {
                removeUniqueIdAssociation();
            }
        } else {
            Ln.w("Closing unknown UHID device: " + id);
        }
    }

    public void closeAll() {
        if (fds.isEmpty()) {
            return;
        }

        for (FileDescriptor fd : fds.values()) {
            close(fd);
        }

        removeUniqueIdAssociation();
    }

    private static void close(FileDescriptor fd) {
        try {
            Os.close(fd);
        } catch (ErrnoException e) {
            Ln.e("Failed to close uhid: " + e.getMessage());
        }
    }

    private boolean mustUseInputPort() {
        return Build.VERSION.SDK_INT >= AndroidVersions.API_33_ANDROID_13 && displayUniqueId != null;
    }

    private void addUniqueIdAssociation() {
        if (mustUseInputPort()) {
            if (Build.VERSION.SDK_INT >= AndroidVersions.API_35_ANDROID_15) {
                ServiceManager.getInputManager().addUniqueIdAssociationByPort(INPUT_PORT, displayUniqueId);
            } else {
                ServiceManager.getInputManager().addUniqueIdAssociation(INPUT_PORT, displayUniqueId);
            }
        }
    }

    private void removeUniqueIdAssociation() {
        if (mustUseInputPort()) {
            if (Build.VERSION.SDK_INT >= AndroidVersions.API_35_ANDROID_15) {
                ServiceManager.getInputManager().removeUniqueIdAssociationByPort(INPUT_PORT);
            } else {
                ServiceManager.getInputManager().removeUniqueIdAssociation(INPUT_PORT);
            }
        }
    }
}