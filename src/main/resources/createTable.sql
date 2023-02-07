-- SQLite --
CREATE TABLE IF NOT EXISTS `pay_order` (
    `id` INTEGER PRIMARY KEY,
    `subject` TEXT NOT NULL,
    `buyer` TEXT NOT NULL,
    `total_fee` NUMERIC NOT NULL,
    `status` INTEGER NOT NULL,
    `create_time` TIMESTAMP NOT NULL,
    `pay_time` TIMESTAMP DEFAULT NULL,
    `pay_info` TEXT DEFAULT NULL
);

-- MySQL --
CREATE TABLE IF NOT EXISTS `pay_order` (
    `id` bigint NOT NULL COMMENT '订单号',
    `subject` varchar(255) NOT NULL COMMENT '订单标题',
    `buyer` varchar(255) NOT NULL COMMENT '买家名称',
    `total_fee` float(11,2) NOT NULL COMMENT '订单金额',
    `status` int(1) NOT NULL COMMENT '订单状态',
    `create_time` datetime NOT NULL COMMENT '订单创建时间',
    `pay_time` datetime DEFAULT NULL COMMENT '订单支付时间',
    `pay_info` text DEFAULT NULL COMMENT '订单支付信息',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;