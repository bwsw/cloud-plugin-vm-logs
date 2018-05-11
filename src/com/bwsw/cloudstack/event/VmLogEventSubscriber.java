package com.bwsw.cloudstack.event;

import com.bwsw.cloudstack.vm.logs.VmLogManager;
import com.cloud.event.EventCategory;
import com.cloud.event.EventTypes;
import com.cloud.vm.VirtualMachine;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.framework.events.EventSubscriber;

import java.lang.reflect.Type;
import java.util.Map;

public class VmLogEventSubscriber implements EventSubscriber {

    private final VmLogManager _vmLogManager;
    private final Gson _gson;
    private final Type _mapType;

    public VmLogEventSubscriber(VmLogManager vmLogManager) {
        if (vmLogManager == null) {
            throw new IllegalArgumentException("Null VmLogService");
        }
        _vmLogManager = vmLogManager;
        _gson = new Gson();
        _mapType = new TypeToken<Map<String, String>>() {
        }.getType();
    }

    public static EventCategory getEventCategory() {
        return EventCategory.ACTION_EVENT;
    }

    public static String getEventType() {
        return EventTypes.EVENT_VM_EXPUNGE;
    }

    public static String getResourceType() {
        return VirtualMachine.class.getSimpleName();
    }

    @Override
    public void onEvent(Event event) {
        if (event.getResourceUUID() != null && getEventType().equals(event.getEventType()) && getEventCategory().equals(EventCategory.getEventCategory(event.getEventCategory()))
                && getResourceType().equals(event.getResourceType())) {
            boolean execute = true;
            if (!Strings.isNullOrEmpty(event.getDescription())) {
                try {
                    Map<String, String> details = _gson.fromJson(event.getDescription(), _mapType);
                    if (details.containsKey("status")) {
                        com.cloud.event.Event.State state = com.cloud.event.Event.State.valueOf(details.get("status"));
                        if (state != com.cloud.event.Event.State.Started) {
                            execute = false;
                        }
                    }
                } catch (Exception e) {
                    // if the status can not be parsed proceed with log deletion
                }

            }
            if (execute) {
                _vmLogManager.deleteVmLogs(event.getResourceUUID());
            }
        }
    }
}
