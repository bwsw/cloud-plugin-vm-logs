package com.bwsw.cloudstack.vm.logs;

import com.google.common.base.Strings;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class VmLogRequestBuilderImpl implements VmLogRequestBuilder {

    private static final String INDEX_PREFIX = "vmlog-";
    private static final String INDEX_SUFFIX = "-*";
    private static final String ID_FIELD = "_id";
    private static final String[] FIELDS = new String[] {LOG_FILE_FIELD, DATA_FIELD, DATE_FIELD};
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Override
    public SearchRequest getLogSearchRequest(String vmUuid, int page, int pageSize, Object[] searchAfter, LocalDateTime start, LocalDateTime end, List<String> keywords,
            String logFile) {
        SearchRequest request = new SearchRequest(getIndex(vmUuid));

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.fetchSource(FIELDS, null);
        sourceBuilder.size(pageSize);

        if (searchAfter != null) {
            sourceBuilder.searchAfter(searchAfter);
        } else {
            sourceBuilder.from((page - 1) * pageSize + 1);
        }

        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        if (start != null || end != null) {
            RangeQueryBuilder dateFilter = QueryBuilders.rangeQuery(DATA_FIELD);
            if (start != null) {
                dateFilter.gte(format(start));
            }
            if (end != null) {
                dateFilter.lt(format(end));
            }
            queryBuilder.filter(dateFilter);
        }
        if (!Strings.isNullOrEmpty(logFile)) {
            queryBuilder.filter(QueryBuilders.termQuery(LOG_FILE_FIELD, logFile));
        }
        if (keywords != null && !keywords.isEmpty()) {
            keywords.forEach(e -> queryBuilder.must(QueryBuilders.matchQuery(DATA_FIELD, e)));

        }
        if (queryBuilder.hasClauses()) {
            sourceBuilder.query(queryBuilder);
        }
        sourceBuilder.sort(new FieldSortBuilder(DATE_FIELD).order(SortOrder.ASC));
        sourceBuilder.sort(new FieldSortBuilder(ID_FIELD).order(SortOrder.ASC));

        request.source(sourceBuilder);
        return request;
    }

    private String getIndex(String vmUuid) {
        return INDEX_PREFIX + vmUuid + INDEX_SUFFIX;
    }

    private String format(LocalDateTime dateTime) {
        return DATE_TIME_FORMATTER.format(dateTime);
    }
}
