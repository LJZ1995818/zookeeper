/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.zookeeper.common;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.slf4j.Logger;

/*
 * This code is originally from HDFS, see the similarly named files there
 * in case of bug fixing, history, etc...
 * 此代码最初来自 HDFS, 请参阅类似命名的文件
 * 如有错误修正, 历史等。
 */

public class IOUtils {
    /**
     * Closes the stream ignoring {@link IOException}. Must only be called in
     * cleaning up from exception handlers.
     * 
     * @param stream
     *            the Stream to close
     */
    public static void closeStream(Closeable stream) {
        cleanup(null, stream);
    }

    /**
     * Close the Closeable objects and <b>ignore</b> any {@link IOException} or
     * null pointers. Must only be used for cleanup in exception handlers.
     * 关闭Closeable对象并忽略任何一个IOException或者空指针。
     * 必须仅用于异常处理程序中的清理。
     * 
     * @param log
     *            the log to record problems to at debug level. Can be null.
     * @param closeables
     *            the objects to close
     */
    public static void cleanup(Logger log, Closeable... closeables) {
        for (Closeable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (IOException e) {
                    if (log != null) {
                        log.warn("Exception in closing " + c, e);
                    }
                }
            }
        }
    }

    /**
     * Copies from one stream to another.
     * 
     * @param in
     *            InputStrem to read from
     * @param out
     *            OutputStream to write to
     * @param buffSize
     *            the size of the buffer
     * @param close
     *            复制完之后，是否关闭stream
     *            whether or not close the InputStream and OutputStream at the
     *            end. The streams are closed in the finally clause.
     */
    public static void copyBytes(InputStream in, OutputStream out,
            int buffSize, boolean close) throws IOException {
        try {
            copyBytes(in, out, buffSize);
            if (close) {
                out.close();
                out = null;
                in.close();
                in = null;
            }
        } finally {
            if (close) {
                closeStream(out);
                closeStream(in);
            }
        }
    }

    /**
     * Copies from one stream to another.
     * 将一个stream反正到另一个stream
     * 
     * @param in
     *            InputStrem to read from
     * @param out
     *            OutputStream to write to
     * @param buffSize
     *            缓存区大小
     */
    public static void copyBytes(InputStream in, OutputStream out, int buffSize)
            throws IOException {
        PrintStream ps = out instanceof PrintStream ? (PrintStream) out : null;
        byte buf[] = new byte[buffSize];
        int bytesRead = in.read(buf);
        while (bytesRead >= 0) {
            out.write(buf, 0, bytesRead);
            if ((ps != null) && ps.checkError()) {
                throw new IOException("Unable to write to output stream.");
            }
            bytesRead = in.read(buf);
        }
    }

}
