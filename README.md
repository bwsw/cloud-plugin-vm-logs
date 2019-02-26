Apache CloudStack Plugin for virtual machine logs
==============

This plugin provides API plugin for Apache CloudStack to process and view virtual machine logs which are handled by ELK and delivered by Filebeat. 
The version of the plugin matches Apache CloudStack version that it is build for.

The plugin is developed and tested only with Apache CloudStack 4.11.2

* [Installing into CloudStack](#installing-into-cloudstack)
* [Plugin settings](#plugin-settings)
* [ELK Configuration](#elk-configuration)
* [API](#api)

# Installing into CloudStack

Download the plugin jar with dependencies file from OSS Nexus (https://oss.sonatype.org/content/groups/public/com/bwsw/cloud-plugin-vm-logs/) which corresponds to your ACS 
version (e.g. 4.11.2.0), put it to lib directory and restart Management server. In Ubuntu installation which is based on deb package:

```
cd /usr/share/cloudstack-management/lib/
wget --trust-server-names "https://oss.sonatype.org/service/local/artifact/maven/redirect?r=snapshots&g=com.bwsw&a=cloud-plugin-vm-logs&c=jar-with-dependencies&v=4.11.2.0-SNAPSHOT"
service cloudstack-management stop
service cloudstack-management start
```
 
# Plugin settings

| Name | Description | Default value |
| -------------- | ----------- | -------- |
| vm.log.elasticsearch.list | comma separated list of ElasticSearch HTTP hosts; e.g. http://localhost,http://localhost:9201 | |
| vm.log.elasticsearch.username | Elasticsearch username for authentication; should be empty if authentication is disabled | |
| vm.log.elasticsearch.password | Elasticsearch password for authentication; should be empty if authentication is disabled | |
| vm.log.page.size.default | the default page size for VM log listing | 100 |
| vm.log.usage.timeout | Timeout in seconds to send VM log statistics | 3600 |

*default.page.size* is used as a default value for pagesize parameter in [listVmLogFiles](#listvmlogfiles) command. Its value should be less or equal to Elasticsearch 
*index.max_result_window* otherwise listVmLogFiles requests without pagesize parameter will fail.
  
# ELK Configuration

Following components should be deployed:

## ElasticSearch

```
Version recommended: 6.2.4
```

The official documentation can be found at https://www.elastic.co/guide/en/elasticsearch/reference/6.2/index.html

Once ElasticSearch is deployed following actions must be done:
 
* to create [VM log template](deployment/vmlog-index-template.json)
* to create `vmlog-registry` index using [settings](deployment/vmlog-registry.json)

If customization for _log_ and _file_ tags in responses for [getVmLogs](#getvmlogs) command is required a new template based on _VM log template_ for an index pattern
*vmlog-** with an adjusted mapping for _message_ and _source_ properties correspondingly should be created.

## Logstash

```
Version recommended: 6.3.2
```

The official documentation can be found at https://www.elastic.co/guide/en/logstash/6.3/index.html.

The [log pipeline](deployment/vmlogs-logstash.conf) should be used for VM log processing.

In the template above following placeholders should be replaced with real values:

| Name | Description |
| -------------- | ---------- |
| %PORT% | the port to process incoming beats from virtual machines |
| %ELASTICSEARCH_HOSTS% | Elasticsearch hosts to store VM logs |
| %VMLOG_REGISTRY_QUERY_TEMPLATE% | file path to [Elasticsearch query template](deployment/vmlog-registry-query-template.json) | 

If SSL or user authentification are required Elasticsearch output plugin should be adjusted (see https://www.elastic.co/guide/en/logstash/6.2/plugins-outputs-elasticsearch.html).

If throttling for VM logs are required Throttle filter plugin should be used (see https://www.elastic.co/guide/en/logstash/6.2/plugins-filters-throttle.html). 

If the token specified in [Filebeat configuration](#filebeat-6.3) is invalid or Elasticsearch is unavailable VM logs will be dropped.

Configuration example:

```
input {
  beats {
    port => 5045
  }
}

filter {
  if [vm_uuid] {
    drop {
    }
  }
  if ![token] {
    drop {
    }
  }
  elasticsearch {
    hosts => "localhost:9200"
    index => "vmlog-registry"
    ssl => false
    tag_on_failure => ["token_failure"]
    fields => {
      "vm_uuid" => "vm_uuid"
    }
    query_template => "/usr/share/logstash/config/vmlog-registry-query-template.json"
  }
  if ![vm_uuid] {
    drop {
    }
  }
  mutate {
    remove_field => ["token"]
  }
}

output {
  elasticsearch {
    hosts => "localhost:9200"
    index => "vmlog-%{[vm_uuid]}-%{+YYYY-MM-dd}"
    ssl => false
  }
}
```

## Filebeat

```
Version recommended: 6.3.2
```


Filebeat should be used in virtual machines for log processing.

The official documentation can be found at https://www.elastic.co/guide/en/beats/filebeat/6.3/index.html

Filebeat configuration should contain a field `token` that is the token obtained via CloudStack (see [createVmLogToken](#createvmlogtoken)), *fields_under_root* equal to true and Logstash output.

Filebeat configuration must not contain a field `vm_uuid` otherwise VM logs will be dropped.

A configuration example can be find [here](deployment/vmlogs-filebeat.yml).

## Curator

```
Version recommended: 5.5.2
```

Curator is used to delete old virtual machine logs.

The official documentation can be found at https://www.elastic.co/guide/en/elasticsearch/client/curator/5.5/index.html

The [action file](deployment/vmlogs-curator.yml) should be used for VM log processing.

In the template following placeholders should be replaced with real values:

| Name | Description |
| -------------- | ---------- |
| %TIMEOUT% | a client timeout in seconds |
| %DAYS% | a number of days to store VM logs |

# API

The plugin provides following API commands to view virtual machine logs:

* [listVmLogFiles](#listvmlogfiles)
* [getVmLogs](#getvmlogs)
* [scrollVmLogs](#scrollvmlogs)
* [createVmLogToken](#createvmlogtoken)
* [invalidateVmLogToken](#invalidatevmlogtoken)

## Commands

### listVmLogFiles

Lists available log files for the virtual machine.

**Request parameters**

| Parameter Name | Description | Required |
| -------------- | ----------- | -------- |
| id | the ID of the virtual machine | true |
| startdate | the start date/time in UTC, yyyy-MM-ddTHH:mm:ss | false |
| enddate | the end date/time in UTC, yyyy-MM-ddTHH:mm:ss | false |
| page | the requested page of the result listing | false |
| pagesize | the size for result listing | false | 

**Response tags**

| Response Name | Description |
| -------------- | ---------- |
| file | the log file name |

### getVmLogs

Retrieves logs for the virtual machine.

**Request parameters**

| Parameter Name | Description | Required |
| -------------- | ----------- | -------- |
| id | the ID of the virtual machine | true |
| startdate | the start date/time in UTC, yyyy-MM-ddTHH:mm:ss | false |
| enddate | the end date/time in UTC, yyyy-MM-ddTHH:mm:ss | false |
| keywords | comma separated list of keywords (AND logical operator is used if multiple keywords are specified) | false |
| logfile | the log file | false |
| sort | comma separated list of response tags optionally prefixed with - for descending order | false |
| page | the requested page of the result listing | false |
| pagesize | the size for result listing | false |
| scroll | timeout in ms for subsequent scroll requests | false | 

If both page/pagesize and scroll parameters are specified scroll is used.

Sorting and filtering for _file_ and _log_ tags in responses is applied to 256 first characters. 
The information how to change the limit can be found at [deployment section](#deployment).  

**Response tags**

See [VM log response tags](#vm-log-response-tags).

### scrollVmLogs

Retrieves next batch of logs for the virtual machine.

**Request parameters**

| Parameter Name | Description | Required |
| -------------- | ----------- | -------- |
| scrollid | the tag to request next batch of logs | true |
| timeout | timeout in ms for subsequent scroll requests | true | 

**Response tags**

See [VM log response tags](#vm-log-response-tags).

### createVmLogToken

Creates a token to publish VM logs.

**Request parameters**

| Parameter Name | Description | Required |
| -------------- | ----------- | -------- |
| id | the ID of the virtual machine | true |

**Response tags**

| Response Name | Description |
| -------------- | ---------- |
| vmlogtoken | the token response |
| &nbsp;&nbsp;&nbsp;&nbsp;token | the token |

### invalidateVmLogToken

**Request parameters**

| Parameter Name | Description | Required |
| -------------- | ----------- | -------- |
| token | the token to publish VM logs | true |

**Response tags**

| Response Name | Description |
| -------------- | ---------- |
| vmlogtokenresult | success response |
| &nbsp;&nbsp;&nbsp;&nbsp;success | true if the token |

## Response tags

### VM log response tags

| Response Name | Description |
| -------------- | ---------- |
| vmlogs | the log listing |
| &nbsp;&nbsp;&nbsp;&nbsp;count | the total number of log entries |
| &nbsp;&nbsp;&nbsp;&nbsp;items(*) | log entries |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;id | the log id |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;timestamp | the date/time of log event registration |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;file | the log file |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;log | the log data |
| &nbsp;&nbsp;&nbsp;&nbsp;scrollid | the tag to request next batch of logs |
