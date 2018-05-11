package com.bwsw.cloudstack.vm.logs.util;

import com.google.common.base.Strings;
import org.apache.http.HttpHost;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpUtils {

    public static Set<HttpHost> getHttpHosts(String hostString) {
        if (Strings.isNullOrEmpty(hostString)) {
            return Collections.emptySet();
        }
        return Stream.of(hostString.split(",")).map(HttpHost::create).collect(Collectors.toSet());
    }
}
