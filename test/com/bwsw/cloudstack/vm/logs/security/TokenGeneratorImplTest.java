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

package com.bwsw.cloudstack.vm.logs.security;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class TokenGeneratorImplTest {

    @Test
    public void testGenerate() throws NoSuchAlgorithmException {
        TokenGeneratorImpl tokenGenerator = new TokenGeneratorImpl();
        List<String> secretKeys = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            secretKeys.add(tokenGenerator.generate());
        }
        secretKeys.forEach(key -> {
            assertNotNull(key);
            assertFalse(key.isEmpty());
        });
        assertEquals(secretKeys.size(), secretKeys.stream().distinct().count());
    }
}
