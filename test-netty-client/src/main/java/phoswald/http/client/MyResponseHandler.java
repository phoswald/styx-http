/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package phoswald.http.client;

import java.util.concurrent.CompletableFuture;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;

class MyResponseHandler extends SimpleChannelInboundHandler<HttpObject> {

    private final StringBuilder text = new StringBuilder();
    private final CompletableFuture<MyResponse> future = new CompletableFuture<>();

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;

            text.append("STATUS: " + response.status() + "\n");
            text.append("VERSION: " + response.protocolVersion() + "\n");
            text.append("\n");

            if (!response.headers().isEmpty()) {
                for (CharSequence name: response.headers().names()) {
                    for (CharSequence value: response.headers().getAll(name)) {
                        text.append("HEADER: " + name + " = " + value + "\n");
                    }
                }
                text.append("\n");
            }

            if (HttpUtil.isTransferEncodingChunked(response)) {
                text.append("CHUNKED CONTENT {\n");
            } else {
                text.append("CONTENT {\n");
            }
        }
        if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;

            text.append(content.content().toString(CharsetUtil.UTF_8));

            if (content instanceof LastHttpContent) {
                text.append("} END OF CONTENT\n");
                future.complete(new MyResponse(text.toString()));
                ctx.close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        future.completeExceptionally(cause);
        ctx.close();
    }

    CompletableFuture<MyResponse> future() {
        return future;
    }
}
