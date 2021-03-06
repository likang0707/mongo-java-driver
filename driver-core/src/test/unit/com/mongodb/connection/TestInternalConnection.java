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

package com.mongodb.connection;

import com.mongodb.MongoException;
import com.mongodb.async.SingleResultCallback;
import org.bson.ByteBuf;
import org.bson.ByteBufNIO;
import org.bson.io.BsonInput;
import org.bson.io.ByteBufferBsonInput;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

class TestInternalConnection implements InternalConnection {

    private static class Interaction {
        private ResponseBuffers responseBuffers;
        private RuntimeException receiveException;
        private RuntimeException sendException;
    }

    private final ConnectionDescription description;
    private final BufferProvider bufferProvider;
    private final Deque<Interaction> replies;
    private final List<BsonInput> sent;
    private boolean opened;
    private boolean closed;

    TestInternalConnection(final ServerId serverId) {
        this.description = new ConnectionDescription(serverId);
        this.bufferProvider = new SimpleBufferProvider();

        this.replies = new LinkedList<Interaction>();
        this.sent = new LinkedList<BsonInput>();
    }

    public void enqueueReply(final ResponseBuffers responseBuffers) {
        Interaction interaction = new Interaction();
        interaction.responseBuffers = responseBuffers;
        replies.add(interaction);
    }

    public void enqueueSendMessageException(final RuntimeException e) {
        Interaction interaction = new Interaction();
        interaction.sendException = e;
        replies.add(interaction);
    }

    public void enqueueReceiveMessageException(final RuntimeException e) {
        Interaction interaction = new Interaction();
        interaction.receiveException = e;
        replies.add(interaction);
    }

    public List<BsonInput> getSent() {
        return sent;
    }

    @Override
    public ConnectionDescription getDescription() {
        return description;
    }

    public void open() {
        opened = true;
    }

    @Override
    public void openAsync(final SingleResultCallback<Void> callback) {
        opened = true;
        callback.onResult(null, null);
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public boolean opened() {
        return opened;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void sendMessage(final List<ByteBuf> byteBuffers, final int lastRequestId) {
        // repackage all byte buffers into a single byte buffer...
        int totalSize = 0;
        for (ByteBuf buf : byteBuffers) {
            totalSize += buf.remaining();
        }

        ByteBuffer combined = ByteBuffer.allocate(totalSize);
        for (ByteBuf buf : byteBuffers) {
            combined.put(buf.array(), 0, buf.remaining());
        }

        combined.flip();

        Interaction interaction = replies.getFirst();
        if (interaction.responseBuffers != null) {
            ReplyHeader header = replaceResponseTo(interaction.responseBuffers.getReplyHeader(), lastRequestId);
            interaction.responseBuffers = (new ResponseBuffers(header, interaction.responseBuffers.getBodyByteBuffer()));

            sent.add(new ByteBufferBsonInput(new ByteBufNIO(combined)));
        } else if (interaction.sendException != null) {
            replies.removeFirst();
            throw interaction.sendException;
        }
    }

    private ReplyHeader replaceResponseTo(final ReplyHeader header, final int responseTo) {
        ByteBuffer headerByteBuffer = ByteBuffer.allocate(36);
        headerByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        headerByteBuffer.putInt(header.getMessageLength());
        headerByteBuffer.putInt(header.getRequestId());
        headerByteBuffer.putInt(responseTo);
        headerByteBuffer.putInt(1);
        headerByteBuffer.putInt(header.getResponseFlags());
        headerByteBuffer.putLong(header.getCursorId());
        headerByteBuffer.putInt(header.getStartingFrom());
        headerByteBuffer.putInt(header.getNumberReturned());
        headerByteBuffer.flip();

        ByteBufferBsonInput headerInputBuffer = new ByteBufferBsonInput(new ByteBufNIO(headerByteBuffer));
        return new ReplyHeader(headerInputBuffer, ConnectionDescription.getDefaultMaxMessageSize());
    }

    @Override
    public ResponseBuffers receiveMessage(final int responseTo) {
        if (this.replies.isEmpty()) {
            throw new MongoException("Test was not setup properly as too many calls to receiveMessage occured.");
        }

        Interaction interaction = replies.removeFirst();
        if (interaction.responseBuffers != null) {
            return interaction.responseBuffers;
        } else {
            throw interaction.receiveException;
        }
    }

    @Override
    public void sendMessageAsync(final List<ByteBuf> byteBuffers, final int lastRequestId, final SingleResultCallback<Void> callback) {
        try {
            sendMessage(byteBuffers, lastRequestId);
            callback.onResult(null, null);
        } catch (RuntimeException e) {
            callback.onResult(null, e);
        }
    }

    @Override
    public void receiveMessageAsync(final int responseTo, final SingleResultCallback<ResponseBuffers> callback) {
        try {
            ResponseBuffers buffers = receiveMessage(responseTo);
            callback.onResult(buffers, null);
        } catch (MongoException ex) {
            callback.onResult(null, ex);
        }
    }

    @Override
    public ByteBuf getBuffer(final int size) {
        return this.bufferProvider.getBuffer(size);
    }
}
