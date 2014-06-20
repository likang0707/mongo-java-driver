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

package org.mongodb.codecs;

import org.bson.BsonBinary;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class ByteArrayCodec implements Codec<byte[]> {
    @Override
    public void encode(final BsonWriter writer, final byte[] value, final EncoderContext encoderContext) {
        writer.writeBinaryData(new BsonBinary(value));
    }

    @Override
    public byte[] decode(final BsonReader reader, final DecoderContext decoderContext) {
        return reader.readBinaryData().getData();
    }

    @Override
    public Class<byte[]> getEncoderClass() {
        return byte[].class;
    }
}
