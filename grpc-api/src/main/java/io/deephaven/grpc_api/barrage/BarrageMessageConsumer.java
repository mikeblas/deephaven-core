package io.deephaven.grpc_api.barrage;

import io.deephaven.db.backplane.barrage.BarrageMessage;
import io.deephaven.db.v2.sources.chunk.ChunkType;

import java.io.InputStream;

public class BarrageMessageConsumer {
    /**
     * Thread safe re-usable reader that converts an InputStreams to BarrageMessages.
     *
     * @param <Options> The options type this StreamReader needs to deserialize.
     */
    public interface StreamReader<Options> {
        /**
         * Converts an InputStream to a BarrageMessage in the context of the provided parameters.
         *
         * @param options          the options related to parsing this message
         * @param columnChunkTypes the types to use for each column chunk
         * @param columnTypes      the actual column type for the column
         * @param stream           the input stream that holds the message to be parsed
         * @return
         */
        BarrageMessage safelyParseFrom(final Options options,
                                       final ChunkType[] columnChunkTypes,
                                       final Class<?>[] columnTypes,
                                       final InputStream stream);
    }
}

