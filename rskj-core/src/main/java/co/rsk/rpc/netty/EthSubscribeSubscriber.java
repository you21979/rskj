/*
 * This file is part of RskJ
 * Copyright (C) 2018 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package co.rsk.rpc.netty;

import co.rsk.rpc.EthSubscribeEventEmitter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * This listens to incoming eth_subscribe and adds listeners.
 * Note that we have to deserialize the request by hand because the JSON RPC library doesn't expose the deserialized
 * objects. Once you call {@link com.googlecode.jsonrpc4j.JsonRpcBasicServer#handleRequest}, the request is out of your
 * hands and you only get an {@link java.io.OutputStream} back.
 * Eventually, we might want to replace the jsonrpc4j library to make things easier.
 */
class EthSubscribeSubscriber extends SimpleChannelInboundHandler<ByteBufHolder> {
    private static final Logger LOGGER = LoggerFactory.getLogger("jsonrpc");

    private final EthSubscribeEventEmitter emitter;

    EthSubscribeSubscriber(EthSubscribeEventEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBufHolder msg) {
        try {
            ObjectMapper mapper = new ObjectMapper().configure(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    false
            );
            JsonNode command = mapper.readValue(
                    (InputStream) new ByteBufInputStream(msg.copy().content()),
                    JsonNode.class
            );

            // TODO
//            if ("eth_unsubscribe".equals(command.get("method").asText())) {
//                byte[] subscriptionId = emitter.subscribe(ctx.channel());
//                ctx.writeAndFlush(new TextWebSocketFrame("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"0x" + Hex.toHexString(subscriptionId) + "\"}"));
//                return;
//            }

            if ("eth_subscribe".equals(command.get("method").asText())) {
                emitter.subscribe(ctx.channel());
                return;
            }
        } catch (IOException e) {
            LOGGER.info("Can't read JSON value for EthSubscribeSubscriber");
        }

        ctx.fireChannelRead(msg.retain());
    }
}
