Apache CloudStack Plugin for virtual machine logs
==============

This project provides API plugin for Apache CloudStack to process and view virtual machine logs.
The version of the plugin matches Apache CloudStack version that it is build for.

* [API](#api)
* [Plugin settings](#plugin-settings)
* [Deployment](#deployment)

# API

The plugin provides following API commands to view virtual machine logs:

* [listVmLogFiles](#listvmlogfiles)
* [getVmLogs](#getvmlogs)
* [scrollVmLogs](#scrollvmlogs)

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
| keywords | keywords (AND operator if multiple keywords are specified) | false |
| logfile | the log file | false |
| sort | comma separated list of response tags optionally prefixed with - for descending order | false |
| page | the requested page of the result listing | false |
| pagesize | the size for result listing | false |
| scroll | timeout in ms for subsequent scroll requests | false | 

If both page/pagesize and scroll parameters are specified scroll is used.

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

## Response tags

### VM log response tags

**Response tags**

| Response Name | Description |
| -------------- | ---------- |
| vmlogs | the log listing |
| &nbsp;&nbsp;&nbsp;&nbsp;count | the total number of log entries |
| &nbsp;&nbsp;&nbsp;&nbsp;items(*) | log entries |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;timestamp | the date/time of log event registration |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;file | the log file |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;log | the log data |
| &nbsp;&nbsp;&nbsp;&nbsp;scrollid | the tag to request next batch of logs |

# Plugin settings

| Name | Description | Default value |
| -------------- | ----------- | -------- |
| vm.log.elasticsearch.list | comma separated list of ElasticSearch HTTP hosts; e.g. http://localhost,http://localhost:9201 | |
| vm.log.elasticsearch.username | Elasticsearch username for authentication; should be empty if authentication is disabled | |
| vm.log.elasticsearch.password | Elasticsearch password for authentication; should be empty if authentication is disabled | |
| vm.log.page.size.default | the default page size for VM log listing | 100 |
  
# Deployment

Following components should be deployed:

* ElasticSearch 6.2

The official documentation can be found at https://www.elastic.co/guide/en/elasticsearch/reference/6.2/index.html

* Logstash 6.2

The official documentation can be found at https://www.elastic.co/guide/en/logstash/6.2/index.html.

The [log pipeline](deployment/vmlogs-logstash.conf) should be used for VM log processing.

In the template above following placeholders should be replaced with real values:

| Name | Description |
| -------------- | ---------- |
| %PORT% | the port to process incoming beats from virtual machines |
| %JDBC_DRIVER_PATH% | the path to JDBC driver library |
| %JDBC_URL% | JDBC connection URL for Apache CloudStack database |
| %JDBC_USER% | the user for Apache CloudStack database |
| %JDBC_PASSWORD% | the user's password for Apache CloudStack database |
| %ELASTICSEARCH_HOSTS% | Elasticsearch hosts to store VM logs |

If SSL or user authentification are required Elasticsearch output plugin should be adjusted (see https://www.elastic.co/guide/en/logstash/6.2/plugins-outputs-elasticsearch.html).

If throttling for VM logs are required Throttle filter plugin should be used (see https://www.elastic.co/guide/en/logstash/6.2/plugins-filters-throttle.html). 

* Filebeat 6.2

Filebeat should be used in virtual machines for log processing.

The official documentation can be found at https://www.elastic.co/guide/en/beats/filebeat/6.2/index.html

Filebeat configuration should contain a field *vm_uuid* that is the ID of the virtual machine, *fields_under_root* equal to true and Logstash output.

A configuration example can be find [here](deployment/vmlogs-filebeat.yml).

* Curator 5.5

Curator is used to delete old virtual machine logs.

The official documentation can be found at https://www.elastic.co/guide/en/elasticsearch/client/curator/5.5/index.html

The [action file](deployment/vmlogs-curator.yml) should be used for VM log processing.

In the template following placeholders should be replaced with real values:

| Name | Description |
| -------------- | ---------- |
| %TIMEOUT% | a client timeout in seconds |
| %DAYS% | a number of days to store VM logs |
