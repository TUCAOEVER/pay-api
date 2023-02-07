package com.promc.payapi.http;

import com.promc.payapi.PayAPI;
import com.promc.payapi.api.payway.Payway;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class HttpServer {

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private FullHttpResponse defaultPage;

    /**
     * 启动服务端
     */
    public void start(int port) {
        close();
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap()
                .option(ChannelOption.SO_BACKLOG, 1024)
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(@NotNull Channel ch) {
                        ch.pipeline()
                                .addLast(new HttpServerCodec())
                                .addLast(new HttpObjectAggregator(65535))
                                .addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {

                                    @Override
                                    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
                                        // 干掉图标
                                        if ("/favicon.ico".equals(request.uri())) {
                                            return;
                                        }
                                        // GET请求 全部发送默认页
                                        if (request.method() == HttpMethod.GET) {
                                            if (defaultPage != null) {
                                                ctx.writeAndFlush(defaultPage.copy()).addListener(ChannelFutureListener.CLOSE);
                                            }
                                            return;
                                        }
                                        // POST请求
                                        if (request.method() == HttpMethod.POST) {
                                            String uri = request.uri();
                                            if (uri.startsWith("/")) {
                                                Optional<Payway> payway = PayAPI.getAPI().getPayway(uri.substring(1));
                                                payway.ifPresent(p -> p.notify(ctx, request));
                                            }
                                        }
                                    }

                                    @Override
                                    public void channelReadComplete(ChannelHandlerContext ctx) {
                                        ctx.flush();
                                    }
                                });
                    }
                });

        try {
            bootstrap.bind(port).sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭HTTP服务器
     */
    public void close() {
        if (bossGroup == null || workerGroup == null) {
            return;
        }
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    /**
     * 设置默认页
     *
     * @param html 内容
     */
    public void setDefaultPage(@NotNull String html) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(html, StandardCharsets.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        defaultPage = response;
    }
}