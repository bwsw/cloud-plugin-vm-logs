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

package com.bwsw.cloudstack.vm.logs.event;

import com.bwsw.cloudstack.vm.logs.service.VmLogManager;
import com.cloud.event.EventCategory;
import com.cloud.server.ManagementService;
import com.cloud.vm.VirtualMachine;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.EventBusException;
import org.hamcrest.CustomMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class VmLogEventManagerImplTest {

    private static final Map<String, Double> STATS = ImmutableMap.of("61d12f36-0201-4035-b6fc-c7f768f583f1", 15.5);
    private static final Set<String> EVENT_DESCRIPTION_FIELDS = ImmutableSet.of(VmLogEventManager.STATS, VmLogEventManager.EVENT_DATE_TIME);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private VmLogManager _vmLogManager;

    @Mock
    private EventBus _eventBus;

    @InjectMocks
    private VmLogEventManagerImpl _vmLogEventManager = new VmLogEventManagerImpl();

    private ObjectMapper _objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    @Test
    public void testPublishVmLogStats() throws EventBusException {
        CustomMatcher<Event> eventMatcher = new CustomMatcher<Event>("eventMatcher") {

            @Override
            public boolean matches(Object o) {
                if (!(o instanceof Event)) {
                    return false;
                }
                Event event = (Event)o;
                if (event.getDescription() == null) {
                    return false;
                }
                try {
                    // check event description
                    Map<String, Object> description = (Map<String, Object>)_objectMapper.readValue(event.getDescription(), Map.class);
                    if (!EVENT_DESCRIPTION_FIELDS.equals(description.keySet())) {
                        return false;
                    }
                    if (!STATS.equals(description.get(VmLogEventManager.STATS))) {
                        return false;
                    }
                    Object eventDate = description.get(VmLogEventManager.EVENT_DATE_TIME);
                    if (!(eventDate instanceof String)) {
                        return false;
                    }
                    Date date = new SimpleDateFormat(VmLogEventManager.EVENT_DATE_FORMAT).parse((String)eventDate);
                    long diff = new Date().getTime() - date.getTime();
                    if (diff < 0 || diff > 1000) {
                        return false;
                    }
                } catch (IOException | ParseException e) {
                    return false;
                }
                // check other event fields
                return ManagementService.Name.equals(event.getEventSource()) && EventCategory.USAGE_EVENT.getName().equals(event.getEventCategory())
                        && EventTypes.EVENT_VM_LOG_STATS.equals(event.getEventType()) && VirtualMachine.class.getSimpleName().equals(event.getResourceType())
                        && event.getResourceUUID() == null;
            }
        };
        doNothing().when(_eventBus).publish(argThat(eventMatcher));

        _vmLogEventManager.publishVmLogStats(STATS);

        verify(_eventBus, times(1)).publish(argThat(eventMatcher));
    }

    @Test
    public void testPublishVmLogStatsEventBusException() throws EventBusException {
        EventBusException exception = new EventBusException("event bus");
        expectedException.expect(exception.getClass());
        expectedException.expectMessage(exception.getMessage());

        doThrow(exception).when(_eventBus).publish(isA(Event.class));

        _vmLogEventManager.publishVmLogStats(STATS);
    }
}
