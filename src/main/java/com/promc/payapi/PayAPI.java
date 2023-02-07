package com.promc.payapi;

import com.comphenix.protocol.ProtocolLibrary;
import com.promc.payapi.api.PayInterface;
import com.promc.payapi.api.order.Order;
import com.promc.payapi.api.payway.Payway;
import com.promc.payapi.api.storage.Storage;
import com.promc.payapi.event.PayCloseEvent;
import com.promc.payapi.event.PayOpenEvent;
import com.promc.payapi.event.PaySuccessEvent;
import com.promc.payapi.event.QRCodeGenerateEvent;
import com.promc.payapi.http.HttpServer;
import com.promc.payapi.listener.PayListener;
import com.promc.payapi.payway.AliPayway;
import com.promc.payapi.payway.TenPayWay;
import com.promc.payapi.payway.WeChatPayway;
import com.promc.payapi.storage.MySqlStorage;
import com.promc.payapi.storage.SQLiteStorage;
import com.promc.payapi.util.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.dependency.Dependency;
import org.bukkit.plugin.java.annotation.plugin.ApiVersion;
import org.bukkit.plugin.java.annotation.plugin.ApiVersion.Target;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import org.bukkit.plugin.java.annotation.plugin.author.Author;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.*;

/**
 * 插件主类
 */
@ApiVersion(Target.v1_13)
@Author("msg_dw")
@Dependency("ProtocolLib")
@Plugin(name = "PayAPI", version = "1.0.0-SNAPSHOT")
public final class PayAPI extends JavaPlugin implements PayInterface {

    /**
     * 雪花算法 用于生成唯一的订单号
     */
    private static final Sequence sequence = new Sequence(0L, 0L);

    /**
     * 雪花算法 用于生成唯一的订单号
     */
    private static PayAPI instance;
    private final List<Payway> paywayList = new ArrayList<>();
    private final Map<UUID, Order> payingOrder = new HashMap<>();
    private String notifyUrl;
    private Storage storage;
    private HttpServer httpServer;
    // 发起支付中
    private Set<UUID> initiatePaying = new HashSet<>();

    /**
     * 获取插件API
     *
     * @return 插件API
     */
    @NotNull
    public static PayAPI getAPI() {
        if (instance == null) {
            throw new IllegalStateException("插件实例未初始化!");
        }
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // 设置debug模式
        LoggerUtil.setDebug(getConfig().getBoolean("debug", false));

        // 注册监听器
        PayListener payListener = new PayListener(this);
        ProtocolLibrary.getProtocolManager().addPacketListener(payListener);
        Bukkit.getPluginManager().registerEvents(payListener, this);

        // 初始化
        setupStorage(getConfig().getConfigurationSection("storage"));
        setupPayway(getConfig().getConfigurationSection("merchant"));
        setupServer(getConfig().getConfigurationSection("server"));
    }

    /**
     * 配置存储方式
     *
     * @param storageConfig 存储配置
     */
    public void setupStorage(ConfigurationSection storageConfig) {
        if (storageConfig == null) {
            storageConfig = new MemoryConfiguration();
        }
        HikariConfig hikariConfig = new HikariConfig();
        if (storageConfig.getString("type", "SQLite").equalsIgnoreCase("MySQL")) {
            String url = String.format("jdbc:mysql://%s:%s/%s",
                    storageConfig.getString("address", "localhost"),
                    storageConfig.getString("port", "3306"),
                    storageConfig.getString("database", "payapi")
            );
            hikariConfig.setJdbcUrl(url);
            hikariConfig.setUsername(storageConfig.getString("username", "root"));
            hikariConfig.setPassword(storageConfig.getString("password", "root"));
            storage = new MySqlStorage(new HikariDataSource(hikariConfig));
        } else {
            String url = String.format("jdbc:sqlite:%s", new File(getDataFolder(), "storage.db").getPath());
            hikariConfig.setJdbcUrl(url);
            storage = new SQLiteStorage(new HikariDataSource(hikariConfig));
        }
        storage.createTable();
    }

    /**
     * 配置支付方式
     *
     * @param merchantConfig 商户配置
     */
    public void setupPayway(ConfigurationSection merchantConfig) {
        if (merchantConfig == null) {
            return;
        }
        ConfigurationSection alipayConfig = merchantConfig.getConfigurationSection("alipay");
        if (alipayConfig != null && alipayConfig.getBoolean("enable")) {
            paywayList.add(new AliPayway(
                    alipayConfig.getString("app-id"),
                    alipayConfig.getString("alipay-public-key"),
                    alipayConfig.getString("merchant-private-key")
            ));
            LoggerUtil.info("加载支付宝商户!");
        }
        ConfigurationSection wechatConfig = merchantConfig.getConfigurationSection("wechat");
        if (wechatConfig != null && wechatConfig.getBoolean("enable")) {
            paywayList.add(new WeChatPayway(
                    wechatConfig.getString("app-id"),
                    wechatConfig.getString("merchant-id"),
                    wechatConfig.getString("merchant-key")
            ));
            LoggerUtil.info("加载微信商户!");
        }
        ConfigurationSection tenpayConfig = merchantConfig.getConfigurationSection("tenpay");
        if (tenpayConfig != null && tenpayConfig.getBoolean("enable")) {
            paywayList.add(new TenPayWay(
                    tenpayConfig.getString("app-id"),
                    tenpayConfig.getString("merchant-id"),
                    tenpayConfig.getString("merchant-key")
            ));
            LoggerUtil.info("加载QQ钱包商户!");
        }
    }

    /**
     * 配置本地服务器
     *
     * @param serverConfig 服务器配置
     */
    public void setupServer(ConfigurationSection serverConfig) {
        if (serverConfig == null) {
            serverConfig = new MemoryConfiguration();
        }
        int port = serverConfig.getInt("port", 8822);
        String notifyUrl = serverConfig.getString("notify", "");
        if (notifyUrl.isEmpty()) {
            try {
                // 如果没有为空就自动匹配以下本地的Ip
                String localhost = InetAddress.getLocalHost().getHostAddress();
                notifyUrl = String.format("http://%s:%d", localhost, port);
            } catch (UnknownHostException e) {
                throw new IllegalStateException(e);
            }
        } else {
            // 去除结尾的 /
            if (notifyUrl.charAt(notifyUrl.length() - 1) == '/') {
                notifyUrl = notifyUrl.substring(notifyUrl.length() - 1);
            }
        }
        this.notifyUrl = notifyUrl;
        httpServer = new HttpServer();
        File file = new File(getDataFolder(), "index.html");
        if (file.exists()) {
            httpServer.setDefaultPage(IOUtil.readString(file).toString());
        } else {
            httpServer.setDefaultPage(IOUtil.readString(getResource("index.html")).toString());
        }
        httpServer.start(port);
        LoggerUtil.info("启动HTTP服务器(" + port + "): " + notifyUrl);
    }

    @Override
    public void onDisable() {
        httpServer.close();
    }

    @Override
    @NotNull
    public String getNotifyUrl() {
        return notifyUrl;
    }

    @Override
    @NotNull
    public Optional<Payway> getPayway(@NotNull String identifier) {
        for (Payway payway : paywayList) {
            if (payway.getIdentifier().equals(identifier)) {
                return Optional.of(payway);
            }
        }
        return Optional.empty();
    }

    @Override
    @NotNull
    public List<Payway> getAllPayway() {
        return paywayList;
    }

    @Override
    public boolean isPaying(@NotNull UUID uuid) {
        return payingOrder.containsKey(uuid);
    }

    @Override
    @NotNull
    public Map<UUID, Order> getPayingOrders() {
        return payingOrder;
    }

    @Override
    @NotNull
    public Order createOrder(@NotNull UUID buyer, @NotNull BigDecimal amount) {
        return createOrder(buyer, amount, "");
    }

    @Override
    @NotNull
    public Order createOrder(@NotNull UUID buyer, @NotNull BigDecimal amount, @NotNull String subject) {
        Order order = new Order();
        order.setId(sequence.nextId());
        order.setBuyer(buyer);
        order.setTotalFee(amount);
        order.setCreateTime(new Timestamp(System.currentTimeMillis()));
        order.setSubject(subject);
        storage.insertOrder(order);
        return order;
    }

    @Override
    public void initiatePay(@NotNull Order order, @NotNull Payway payway) {
        Player player = Bukkit.getPlayer(order.getBuyer());
        if (player == null) {
            throw new IllegalStateException("玩家不在线");
        }

        if (order.getStatus() != 0) {
            throw new IllegalStateException("订单状态异常");
        }

        if (isPaying(order.getBuyer())) {
            throw new IllegalStateException("玩家正在支付");
        }

        if (initiatePaying.contains(order.getBuyer())) {
            throw new IllegalStateException("请稍后再试");
        }
        initiatePaying.add(order.getBuyer());

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            String qrcodeUrl = payway.nativePay(order);
            if (qrcodeUrl != null) {
                BufferedImage image = FastQRCodeUtil.createQrCode(qrcodeUrl);
                QRCodeGenerateEvent qrCodeGenerateEvent = new QRCodeGenerateEvent(this, order, qrcodeUrl, image);
                Bukkit.getPluginManager().callEvent(qrCodeGenerateEvent);
                BufferedImage qrCodeImage = qrCodeGenerateEvent.getQRCodeImage();
                MapUtil.sendMapItemPacket(player, MapUtil.buildMapItem());
                MapUtil.sendMapViewPacket(player, qrCodeImage);
                payingOrder.put(player.getUniqueId(), order);
                Bukkit.getScheduler().runTask(this, () -> {
                    PayOpenEvent payOpenEvent = new PayOpenEvent(this, order);
                    Bukkit.getPluginManager().callEvent(payOpenEvent);
                });
            }
            initiatePaying.remove(order.getBuyer());
        });
    }

    @Override
    public void markPay(Map<String, String> orderInfo) {
        String outTradeNo = orderInfo.get("out_trade_no");
        long orderId = Long.parseLong(outTradeNo);
        if (storage.markPay(orderId, GSON.toJson(orderInfo))) {
            Order order = storage.selectOrderById(orderId);
            Player player = Bukkit.getPlayer(order.getBuyer());
            if (player != null) {
                if (payingOrder.remove(player.getUniqueId()) != null) {
                    Bukkit.getScheduler().runTask(this, () -> {
                        PayCloseEvent payCloseEvent = new PayCloseEvent(this, order);
                        Bukkit.getPluginManager().callEvent(payCloseEvent);
                    });
                }
                player.updateInventory();
            }
            // 拉回主线程处理支付成功
            Bukkit.getScheduler().runTask(this, () -> {
                PaySuccessEvent paySuccessEvent = new PaySuccessEvent(this, order);
                Bukkit.getPluginManager().callEvent(paySuccessEvent);
            });
        }
    }

    @Override
    @NotNull
    public Storage getStorage() {
        return storage;
    }
}