package com.promc.payapi.util;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 用于快速生成二维码
 * 使用ECI UTF-8作为数据编码,少部分扫码器可能无法识别,但是可以使用中文以及更好的性能(下面有说明)
 * 固定二维码版本为6(比较合适的尺寸41*41) 纠错等级为H(最大纠错等级) 掩码使用1(可以选择其它的 但不支持动态)
 * 地图尺寸为128 41*41的二维码放大三倍后123*123剩下5个像素留作边框(比较合适)
 */
public class FastQRCodeUtil {

    /**
     * 定位图像
     * 大方块
     */
    private static final byte[][] POSITION_DETECTION_PATTERN = {
            {1, 1, 1, 1, 1, 1, 1},
            {1, 0, 0, 0, 0, 0, 1},
            {1, 0, 1, 1, 1, 0, 1},
            {1, 0, 1, 1, 1, 0, 1},
            {1, 0, 1, 1, 1, 0, 1},
            {1, 0, 0, 0, 0, 0, 1},
            {1, 1, 1, 1, 1, 1, 1}
    };

    /**
     * 辅助定位图像
     * 小方块
     */
    private static final byte[][] POSITION_ADJUSTMENT_PATTERN = {
            {1, 1, 1, 1, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 1, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 1, 1, 1, 1}
    };

    /**
     * 基板
     * 存储二维码的基本图像
     * 固定版本为6 尺寸为41*41 共有三个定位图像和一个辅助定位图像
     */
    private static final byte[][] TEMPLATE = new byte[41][41];

    /**
     * 里德所罗门算法
     * 版本6 纠错等级H 每块都有28个纠错码
     */
    private static final ReedSolomonEncoder REED_SOLOMON_ENCODER = new ReedSolomonEncoder(28);

    static {
        // 初始化基板 全部填充-1
        for (byte[] rows : TEMPLATE) {
            Arrays.fill(rows, (byte) -1);
        }

        // 三个大方块
        for (int i = 0; i < POSITION_DETECTION_PATTERN.length; i++) {
            for (int j = 0; j < POSITION_DETECTION_PATTERN[i].length; j++) {
                TEMPLATE[i][j] = POSITION_DETECTION_PATTERN[i][j];
                TEMPLATE[i][34 + j] = POSITION_DETECTION_PATTERN[i][j];
                TEMPLATE[34 + i][j] = POSITION_DETECTION_PATTERN[i][j];
            }
        }
        // 三个大方块的白边
        for (int i = 0; i < 8; i++) {
            TEMPLATE[i][7] = 0;
            TEMPLATE[7][i] = 0;
            TEMPLATE[i][33] = 0;
            TEMPLATE[33][i] = 0;
            TEMPLATE[33 + i][7] = 0;
            TEMPLATE[7][33 + i] = 0;
        }
        // 三个大方块的连接符
        for (int i = 8; i < TEMPLATE.length - 8; i++) {
            byte j = (byte) (i % 2 == 0 ? 1 : 0);
            TEMPLATE[i][6] = j;
            TEMPLATE[6][i] = j;
        }
        // 一个小方块
        for (int i = 0; i < POSITION_ADJUSTMENT_PATTERN.length; i++) {
            System.arraycopy(POSITION_ADJUSTMENT_PATTERN[i], 0, TEMPLATE[32 + i], 32, POSITION_ADJUSTMENT_PATTERN[i].length);
        }
        // 一个小点
        TEMPLATE[33][8] = 1;
    }

    /**
     * 工具类隐藏构造方法
     */
    private FastQRCodeUtil() {
        throw new UnsupportedOperationException("此类不允许实例化!");
    }


    /**
     * 创建二维码基板
     * 使用版本6 纠错等级H 掩码1
     *
     * @return 二维码基板
     */
    public static byte[][] createQrCodeTemplate() {
        byte[][] bytes = new byte[TEMPLATE.length][];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = TEMPLATE[i].clone();
        }
        return bytes;
    }

    /**
     * 创建二维码
     *
     * @param content 内容
     * @return 二维码图像
     */
    public static BufferedImage createQrCode(@NotNull String content) {
        return toBufferedImage(generateQrCode(content), 128);
    }

    /**
     * 生成二维码
     *
     * @param content 内容
     * @return 二维数组
     */
    public static byte[][] generateQrCode(@NotNull String content) {
        return generateQrCode(content, Mask.MASK_1);
    }

    /**
     * 生成二维码
     *
     * @param content 内容
     * @param mask    选择一个掩码类型效率会更高,但是不利于识别
     * @return 二维数组
     */
    public static byte[][] generateQrCode(@NotNull String content, @NotNull Mask mask) {
        // 将数据填入 矩阵
        byte[][] matrix = createQrCodeTemplate();
        writeData(matrix, generateFinalCode(generateDataCode(content)), mask);
        return matrix;
    }

    /**
     * 将二维数组转为黑白图像
     *
     * @param bytes 二维数组(二维码)
     * @return 图像
     */
    public static BufferedImage toBufferedImage(byte[][] bytes, int size) {
        int byteArraySize = bytes.length;

        // 图像放大倍数
        int multiple = size / byteArraySize;
        // 计算剩余边界
        int margin = (size - multiple * byteArraySize) / 2;

        // 生成二维码 TYPE_BYTE_BINARY 用于压缩图片体积
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_BINARY);

        // 画布
        Graphics graphics = image.getGraphics();
        graphics.setColor(new Color(0xFFFFFFFF));
        // 填充白底
        graphics.fillRect(0, 0, size, size);
        graphics.setColor(new Color(0xFF000000));
        // 填充黑块 为1填充 0和-1都不填充
        for (int i = 0; i < bytes.length; i++) {
            for (int j = 0; j < bytes[i].length; j++) {
                if (bytes[i][j] == 1) {
                    graphics.fillRect(j * multiple + margin, i * multiple + margin, multiple, multiple);
                }
            }
        }
        graphics.dispose();
        image.flush();
        return image;
    }

    /**
     * 生成数据码
     * 使用ECI模式下的UTF-8编码,支持中文
     * 版本6 纠错等级H 总共60byte数据码
     *
     * @param content 内容
     * @return 二进制数组
     */
    private static int[] generateDataCode(String content) {
        // 使用60个int存储二进制 每个int只存储1byte(bit)数据
        int[] bits = new int[60];
        // 字符串使用UTF-8字符集编码
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        // 写入头信息
        // 0111 代表 ECI模式
        // 00011010 代表 26号 UTF-8 字符集
        // 0100 代表 BYTE模式读取
        // 不使用ECI模式的话 头信息才4bit 不方便后续插入
        bits[0] = 0b01110001;
        bits[1] = 0b10100100;
        // 插入数据长度
        bits[2] = contentBytes.length;

        // 插入数据
        for (int i = 0; i < contentBytes.length; i++) {
            bits[i + 3] = contentBytes[i] & 0xFF;
        }

        // 写入终止符 0000 补了 8bit
        // 如果正好60byte不足以插入终止符可以省略
        int endPos = 3 + contentBytes.length;
        if (endPos < 60) {
            bits[endPos] = 0;
        }

        // 最后用0xEC(11101100)和0x11(00010001)交替补齐数据位
        for (int i = endPos + 1, x = 0; i < bits.length; i++, x++) {
            bits[i] = x % 2 == 0 ? 0xEC : 0x11;
        }

        return bits;
    }

    /**
     * 生成最终编码
     * 版本6 纠错等级H 总共60byte数据位 112byte纠错位
     *
     * @param dataCode 数据码
     * @return 数据码+纠错码
     */
    private static int[] generateFinalCode(int[] dataCode) {
        int[] bits = new int[172];

        // 拆分成四分 每份十五个数据位
        int[] dataBytes1 = new int[15];
        int[] dataBytes2 = new int[15];
        int[] dataBytes3 = new int[15];
        int[] dataBytes4 = new int[15];
        System.arraycopy(dataCode, 0, dataBytes1, 0, 15);
        System.arraycopy(dataCode, 15, dataBytes2, 0, 15);
        System.arraycopy(dataCode, 30, dataBytes3, 0, 15);
        System.arraycopy(dataCode, 45, dataBytes4, 0, 15);

        // 单独计算每一份的纠错码
        int[] bytes1 = REED_SOLOMON_ENCODER.encode(dataBytes1);
        int[] bytes2 = REED_SOLOMON_ENCODER.encode(dataBytes2);
        int[] bytes3 = REED_SOLOMON_ENCODER.encode(dataBytes3);
        int[] bytes4 = REED_SOLOMON_ENCODER.encode(dataBytes4);

        // 交叉排列
        for (int i = 0; i < 43; i++) {
            int x = i * 4;
            bits[x] = bytes1[i];
            bits[x + 1] = bytes2[i];
            bits[x + 2] = bytes3[i];
            bits[x + 3] = bytes4[i];
        }

        return bits;
    }

    /**
     * 写入数据
     *
     * @param matrix 二维码矩阵
     * @param bits   最终的数据码
     */
    private static void writeData(byte[][] matrix, int[] bits, Mask mask) {
        // 写入格式信息
        mask.writeFormatInformation(matrix);
        // 索引号 成功写入一个数据之后 会 +1
        int index = 0;
        // 方向 true是往上 false是往下
        boolean direction = true;
        // 从右下角开始插入数据 版本为6 二维码固定为
        int x = 40;
        int y = 40;
        while (true) {
            // 如果是空数据 则写入数据
            if (matrix[x][y] == -1) {
                boolean b = getBit(bits, index++) ^ mask.getMashBit(x, y);
                matrix[x][y] = (byte) (b ? 1 : 0);
            }
            if (matrix[x][y - 1] == -1) {
                boolean b = getBit(bits, index++) ^ mask.getMashBit(x, y - 1);
                matrix[x][y - 1] = (byte) (b ? 1 : 0);
            }
            // 如果到边界了 往左走两步 然后修改方向
            if (x == (direction ? 0 : 40)) {
                y -= 2;
                // 如果 到分割线的位置 则再进一步
                if (y == 6) {
                    y--;
                }
                // 如果小于0 则结束
                if (y < 0) {
                    break;
                }
                direction = !direction;
            } else {
                // 每次移动如果方向是向上 则 -1 向下 则 +1
                int move = direction ? -1 : 1;
                x += move;
                if (x == 6) {
                    x += move;
                }
            }
        }
    }

    /**
     * 获取一位bit
     *
     * @param bits  bits
     * @param index 索引
     * @return 一位bit
     */
    private static boolean getBit(int[] bits, int index) {
        return index < bits.length * 8 && ((bits[index / 8] >> 7 - index % 8) & 1) != 0;
    }

    /**
     * 二维码的掩码
     * 避免二维码出现很多连续白块或黑块 降低识别率
     */
    public enum Mask {
        //
        MASK_0(0, 0b001011010001001) {
            @Override
            public boolean getMashBit(int row, int column) {
                return ((row + column) & 1) == 0;
            }
        },
        MASK_1(1, 0b001001110111110) {
            @Override
            public boolean getMashBit(int row, int column) {
                return (row & 1) == 0;
            }
        },
        MASK_2(2, 0b001110011100111) {
            @Override
            public boolean getMashBit(int row, int column) {
                return column % 3 == 0;
            }
        },
        MASK_3(3, 0b001100111010000) {
            @Override
            public boolean getMashBit(int row, int column) {
                return (row + column) % 3 == 0;
            }
        },
        MASK_4(4, 0b000011101100010) {
            @Override
            public boolean getMashBit(int row, int column) {
                return (row / 2 + column / 3 & 1) == 0;
            }
        },
        MASK_5(5, 0b000001001010101) {
            @Override
            public boolean getMashBit(int row, int column) {
                return ((row * column) & 1) + (row * column) % 3 == 0;
            }
        },
        MASK_6(6, 0b000110100001100) {
            @Override
            public boolean getMashBit(int row, int column) {
                return (((row * column) & 1) + (row * column) % 3 & 1) == 0;
            }
        },
        MASK_7(7, 0b000100000111011) {
            @Override
            public boolean getMashBit(int row, int column) {
                return ((row + column & 1) + row * column % 3 & 1) == 0;
            }
        };

        private final int code;
        private final int formatInformation;

        Mask(int code, int formatInformation) {
            this.code = code;
            this.formatInformation = formatInformation;
        }

        public int getCode() {
            return code;
        }

        /**
         * 写入格式信息
         * 格式信息与纠错等级和掩码类型有关
         * 纠错等级固定为H 每个掩码都有一个唯一的格式信息
         *
         * @param matrix 二维码矩阵
         */
        public void writeFormatInformation(byte[][] matrix) {
            for (int i = 0; i < 15; i++) {
                byte b = (byte) ((formatInformation >> 14 - i) & 1);

                if (i < 7) {
                    // 前七位
                    int y = i >= 6 ? i + 1 : i;
                    matrix[8][y] = b;
                    matrix[40 - i][8] = b;
                } else {
                    // 后八位
                    int x = i > 8 ? 14 - i : 15 - i;
                    matrix[x][8] = b;
                    matrix[8][26 + i] = b;
                }
            }
        }

        /**
         * 获取掩码位
         *
         * @param row    行
         * @param column 列
         * @return 掩码位
         */
        public abstract boolean getMashBit(int row, int column);
    }

    /**
     * 里德所罗门算法 - 计算纠错码
     * <p>
     * 参考: https://en.wikiversity.org/wiki/Reed%E2%80%93Solomon_codes_for_coders
     */
    public static class ReedSolomonEncoder {

        /**
         * GF(256) 指数表
         */
        private static final int[] GF_EXP = new int[256];
        private static final int[] GF_LOG = new int[256];

        private final int[] poly;

        static {
            int x = 1;
            for (int i = 0; i < 255; i++) {
                GF_EXP[i] = x;
                GF_LOG[x] = i;
                // 位运算 x <<= 1 约等于 x = x * 2
                // 0x100 = 256 0x011D = 285
                x <<= 1;
                if (x >= 0x100) {
                    x ^= 0x011D;
                }
            }
        }

        /**
         * @param ecLength 纠错码长度
         */
        public ReedSolomonEncoder(int ecLength) {
            poly = generatorPoly(ecLength);
        }

        /**
         * 里德所罗门算法编码
         *
         * @param data 数据码
         * @return 包含数据码和纠错码的数组
         */
        public int[] encode(int[] data) {
            // 最终结果数组
            int[] result = new int[data.length + poly.length - 1];

            // 前面放数据码
            System.arraycopy(data, 0, result, 0, data.length);

            // 计算纠错码
            for (int i = 0; i < data.length; i++) {
                int c = result[i];

                if (c != 0) {
                    for (int j = 1; j < poly.length; j++) {
                        result[i + j] ^= mul(poly[j], c);
                    }
                }
            }

            // 再来一次 覆盖前面的商
            System.arraycopy(data, 0, result, 0, data.length);

            return result;
        }

        /**
         * 生成多项式
         *
         * @param length 长度
         * @return length + 1个校验符号(用于后续计算)
         */
        public int[] generatorPoly(int length) {
            int[] g = new int[]{1};
            for (int i = 0; i < length; i++) {
                g = polyMul(g, new int[]{1, GF_EXP[i]});
            }
            return g;
        }

        /**
         * 多项式乘法
         */
        public int[] polyMul(int[] p, int[] q) {
            int[] r = new int[p.length + q.length - 1];
            for (int i = 0; i < q.length; i++) {
                for (int j = 0; j < p.length; j++) {
                    r[i + j] ^= mul(p[j], q[i]);
                }
            }
            return r;
        }

        /**
         * 加瓦罗域中的模二乘法
         * 其实就是查表
         *
         * @return 结果
         */
        public int mul(int x, int y) {
            if (x == 0 || y == 0) {
                return 0;
            }
            return GF_EXP[(GF_LOG[x] + GF_LOG[y]) % 255];
        }
    }
}
