/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.fielddata.plain;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.OrdinalMap;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedSetSelector;
import org.apache.lucene.search.SortedSetSortField;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.fielddata.AtomicOrdinalsFieldData;
import org.elasticsearch.index.fielddata.IndexFieldDataCache;
import org.elasticsearch.index.fielddata.IndexOrdinalsFieldData;
import org.elasticsearch.index.fielddata.NullValueOrder;
import org.elasticsearch.index.fielddata.ordinals.GlobalOrdinalsBuilder;
import org.elasticsearch.indices.breaker.CircuitBreakerService;
import org.elasticsearch.search.MultiValueMode;

import java.io.IOException;

public class SortedSetDVOrdinalsIndexFieldData extends DocValuesIndexFieldData implements IndexOrdinalsFieldData {

    private final IndexSettings indexSettings;
    private final IndexFieldDataCache cache;
    private final CircuitBreakerService breakerService;
    private static final Logger logger = LogManager.getLogger(SortedSetDVOrdinalsIndexFieldData.class);

    public SortedSetDVOrdinalsIndexFieldData(IndexSettings indexSettings, IndexFieldDataCache cache, String fieldName,
            CircuitBreakerService breakerService) {
        super(indexSettings.getIndex(), fieldName);
        this.indexSettings = indexSettings;
        this.cache = cache;
        this.breakerService = breakerService;
    }

    @Override
    public SortField sortField(NullValueOrder nullValueOrder, MultiValueMode sortMode, boolean reverse) {
        SortField sortField = new SortedSetSortField(fieldName, reverse,
            sortMode == MultiValueMode.MAX ? SortedSetSelector.Type.MAX : SortedSetSelector.Type.MIN);
        sortField.setMissingValue(
            nullValueOrder == NullValueOrder.LAST ^ reverse
                ? SortedSetSortField.STRING_LAST
                : SortedSetSortField.STRING_FIRST);
        return sortField;
    }

    @Override
    public AtomicOrdinalsFieldData load(LeafReaderContext context) {
        return new SortedSetDVBytesAtomicFieldData(context.reader(), fieldName);
    }

    @Override
    public AtomicOrdinalsFieldData loadDirect(LeafReaderContext context) throws Exception {
        return load(context);
    }

    @Override
    public IndexOrdinalsFieldData loadGlobal(DirectoryReader indexReader) {
        if (indexReader.leaves().size() <= 1) {
            // ordinals are already global
            return this;
        }
        boolean fieldFound = false;
        for (LeafReaderContext context : indexReader.leaves()) {
            if (context.reader().getFieldInfos().fieldInfo(getFieldName()) != null) {
                fieldFound = true;
                break;
            }
        }
        if (fieldFound == false) {
            // Some directory readers may be wrapped and report different set of fields and use the same cache key.
            // If a field can't be found then it doesn't mean it isn't there,
            // so if a field doesn't exist then we don't cache it and just return an empty field data instance.
            // The next time the field is found, we do cache.
            try {
                return GlobalOrdinalsBuilder.buildEmpty(indexSettings, indexReader, this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            return cache.load(indexReader, this);
        } catch (Exception e) {
            if (e instanceof ElasticsearchException) {
                throw (ElasticsearchException) e;
            } else {
                throw new ElasticsearchException(e);
            }
        }
    }

    @Override
    public IndexOrdinalsFieldData localGlobalDirect(DirectoryReader indexReader) throws Exception {
        return GlobalOrdinalsBuilder.build(indexReader, this, indexSettings, breakerService, logger);
    }

    @Override
    public OrdinalMap getOrdinalMap() {
        return null;
    }
}
