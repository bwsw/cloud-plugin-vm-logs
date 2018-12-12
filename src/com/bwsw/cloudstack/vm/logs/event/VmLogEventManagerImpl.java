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
import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.EventBusException;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.bwsw.cloudstack.vm.logs.service.VmLogManager.VmLogUsageTimeout;

public class VmLogEventManagerImpl extends ComponentLifecycleBase implements VmLogEventManager {

    private static final Logger s_logger = Logger.getLogger(VmLogEventManagerImpl.class);

    private static final String TIMER_NAME = "VM_LOG_USAGE";

    private class VmLogUsageTask extends TimerTask {

        @Override
        public void run() {
            s_logger.info("Update of VM log statistics started");
            try {
                publishVmLogStats(_vmLogManager.getVmLogStats());
                s_logger.info("Update of VM log statistics finished");
            } catch (Exception e) {
                s_logger.error("Unable to update VM log statistics", e);
            }
        }
    }

    @Inject
    private EventBus _eventBus;

    @Inject
    private VmLogManager _vmLogManager;

    private Timer _timer;

    @Override
    public void publishVmLogStats(Map<String, Double> stats) throws EventBusException {
        Event event = new Event(EVENT_SOURCE, EventCategory.USAGE_EVENT.getName(), EventTypes.EVENT_VM_LOG_STATS, VirtualMachine.class.getSimpleName(), null);
        Map<String, Object> details = new HashMap<>();
        String eventDate = new SimpleDateFormat(EVENT_DATE_FORMAT).format(new Date());
        details.put(EVENT_DATE_TIME, eventDate);
        details.put(STATS, stats);
        event.setDescription(details);
        _eventBus.publish(event);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _timer = new Timer(TIMER_NAME);
        return super.configure(name, params);
    }

    @Override
    public boolean start() {
        long timeout = VmLogUsageTimeout.value() * 1000;
        _timer.schedule(new VmLogUsageTask(), timeout, timeout);
        return super.start();
    }

    @Override
    public boolean stop() {
        _timer.cancel();
        return super.stop();
    }
}
