package com.bwsw.cloudstack.event;

import com.bwsw.cloudstack.vm.logs.VmLogManager;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.EventBusException;
import org.apache.cloudstack.framework.events.EventTopic;

import javax.inject.Inject;

public class EventSubscriberFactoryImpl implements EventSubscriberFactory {

    @Inject
    private EventBus _eventBus;

    @Inject
    private VmLogManager _vmLogManager;

    public VmLogEventSubscriber getVmLogEventSubscriber() throws EventBusException {
        VmLogEventSubscriber subscriber = new VmLogEventSubscriber(_vmLogManager);
        EventTopic eventTopic = new EventTopic(VmLogEventSubscriber.getEventCategory().getName(), VmLogEventSubscriber.getEventType(), null, null, null);
        _eventBus.subscribe(eventTopic, subscriber);
        return subscriber;
    }
}
