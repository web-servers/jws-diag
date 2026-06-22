package org.jboss.jws.diag.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Serializes {@link Path} as a forward-slash string on all platforms.
 *
 * <p>{@code Path.toString()} uses the OS separator, producing backslashes on Windows.
 * This serializer normalizes to forward slashes so JSON output is portable.
 */
public final class UnixPathSerializer extends StdSerializer<Path> {

    public UnixPathSerializer() {
        super(Path.class);
    }

    @Override
    public void serialize(Path value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeString(value.toString().replace(File.separatorChar, '/'));
    }
}
