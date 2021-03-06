/*
 * Copyright (c) 2008-2014 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.async.client;

import com.mongodb.CursorType;
import com.mongodb.MongoNamespace;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.async.AsyncBatchCursor;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.FindOptions;
import com.mongodb.internal.operation.AsyncOperations;
import com.mongodb.operation.AsyncOperationExecutor;
import com.mongodb.operation.AsyncReadOperation;
import com.mongodb.session.ClientSession;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.mongodb.assertions.Assertions.notNull;


@SuppressWarnings("deprecation")
class FindIterableImpl<TDocument, TResult> extends MongoIterableImpl<TResult> implements FindIterable<TResult> {
    private final AsyncOperations<TDocument> operations;
    private final Class<TResult> resultClass;
    private final FindOptions findOptions;

    private Bson filter;

    FindIterableImpl(final ClientSession clientSession, final MongoNamespace namespace, final Class<TDocument> documentClass,
                     final Class<TResult> resultClass, final CodecRegistry codecRegistry, final ReadPreference readPreference,
                     final ReadConcern readConcern, final AsyncOperationExecutor executor, final Bson filter,
                     final FindOptions findOptions) {
        super(clientSession, executor, readConcern, readPreference);
        this.operations = new AsyncOperations<TDocument>(namespace, documentClass, readPreference, codecRegistry, readConcern);
        this.resultClass = notNull("resultClass", resultClass);
        this.filter = notNull("filter", filter);
        this.findOptions = notNull("findOptions", findOptions);
    }

    @Override
    public FindIterable<TResult> filter(final Bson filter) {
        this.filter = filter;
        return this;
    }

    @Override
    public FindIterable<TResult> limit(final int limit) {
        findOptions.limit(limit);
        return this;
    }

    @Override
    public FindIterable<TResult> skip(final int skip) {
        findOptions.skip(skip);
        return this;
    }

    @Override
    public FindIterable<TResult> maxTime(final long maxTime, final TimeUnit timeUnit) {
        notNull("timeUnit", timeUnit);
        findOptions.maxTime(maxTime, timeUnit);
        return this;
    }

    @Override
    public FindIterable<TResult> maxAwaitTime(final long maxAwaitTime, final TimeUnit timeUnit) {
        notNull("timeUnit", timeUnit);
        findOptions.maxAwaitTime(maxAwaitTime, timeUnit);
        return this;
    }

    @Override
    public FindIterable<TResult> batchSize(final int batchSize) {
        findOptions.batchSize(batchSize);
        return this;
    }

    @Override
    public FindIterable<TResult> collation(final Collation collation) {
        findOptions.collation(collation);
        return this;
    }

    @Override
    public FindIterable<TResult> modifiers(final Bson modifiers) {
        findOptions.modifiers(modifiers);
        return this;
    }

    @Override
    public FindIterable<TResult> projection(final Bson projection) {
        findOptions.projection(projection);
        return this;
    }

    @Override
    public FindIterable<TResult> sort(final Bson sort) {
        findOptions.sort(sort);
        return this;
    }

    @Override
    public FindIterable<TResult> noCursorTimeout(final boolean noCursorTimeout) {
        findOptions.noCursorTimeout(noCursorTimeout);
        return this;
    }

    @Override
    public FindIterable<TResult> oplogReplay(final boolean oplogReplay) {
        findOptions.oplogReplay(oplogReplay);
        return this;
    }

    @Override
    public FindIterable<TResult> partial(final boolean partial) {
        findOptions.partial(partial);
        return this;
    }

    @Override
    public FindIterable<TResult> cursorType(final CursorType cursorType) {
        findOptions.cursorType(cursorType);
        return this;
    }

    @Override
    public FindIterable<TResult> comment(final String comment) {
        findOptions.comment(comment);
        return this;
    }

    @Override
    public FindIterable<TResult> hint(final Bson hint) {
        findOptions.hint(hint);
        return this;
    }

    @Override
    public FindIterable<TResult> max(final Bson max) {
        findOptions.max(max);
        return this;
    }

    @Override
    public FindIterable<TResult> min(final Bson min) {
        findOptions.min(min);
        return this;
    }

    @Override
    public FindIterable<TResult> maxScan(final long maxScan) {
        findOptions.maxScan(maxScan);
        return this;
    }

    @Override
    public FindIterable<TResult> returnKey(final boolean returnKey) {
        findOptions.returnKey(returnKey);
        return this;
    }

    @Override
    public FindIterable<TResult> showRecordId(final boolean showRecordId) {
        findOptions.showRecordId(showRecordId);
        return this;
    }

    @Override
    public FindIterable<TResult> snapshot(final boolean snapshot) {
        findOptions.snapshot(snapshot);
        return this;
    }

    @Override
    public void first(final SingleResultCallback<TResult> callback) {
        notNull("callback", callback);
        getExecutor().execute(operations.findFirst(filter, resultClass, findOptions), getReadPreference(), getClientSession(),
                new SingleResultCallback<AsyncBatchCursor<TResult>>() {
            @Override
            public void onResult(final AsyncBatchCursor<TResult> batchCursor, final Throwable t) {
                if (t != null) {
                    callback.onResult(null, t);
                } else {
                    batchCursor.next(new SingleResultCallback<List<TResult>>() {
                        @Override
                        public void onResult(final List<TResult> result, final Throwable t) {
                            batchCursor.close();
                            if (t != null) {
                                callback.onResult(null, t);
                            } else if (result == null || result.isEmpty()) {
                                callback.onResult(null, null);
                            } else {
                                callback.onResult(result.get(0), null);
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    AsyncReadOperation<AsyncBatchCursor<TResult>> asAsyncReadOperation() {
        return createFindOperation();
    }

    private AsyncReadOperation<AsyncBatchCursor<TResult>> createFindOperation() {
        return operations.find(filter, resultClass, findOptions);
    }
}
