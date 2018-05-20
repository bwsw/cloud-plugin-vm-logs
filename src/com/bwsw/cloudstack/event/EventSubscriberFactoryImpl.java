// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

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
