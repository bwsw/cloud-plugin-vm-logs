package com.bwsw.cloudstack.event;

import org.apache.cloudstack.framework.events.EventBusException;

public interface EventSubscriberFactory {

    VmLogEventSubscriber getVmLogEventSubscriber() throws EventBusException;
}
