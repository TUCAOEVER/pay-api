package com.promc.payapi.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class IOUtil {

    /**
     * 工具类隐藏构造方法
     */
    private IOUtil() {
        throw new UnsupportedOperationException("此类不允许实例化!");
    }

    /**
     * 读字符串
     *
     * @param file 文件
     * @return 字符串
     */
    public static StringBuilder readString(File file) {
        return readString(file, StandardCharsets.UTF_8);
    }

    /**
     * 读字符串
     *
     * @param file    文件
     * @param charset 字符集
     * @return 字符串
     */
    public static StringBuilder readString(File file, Charset charset) {
        try {
            return readString(new FileInputStream(file), charset);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 读字符串
     *
     * @param inputStream 输入流
     * @return 字符串
     */
    public static StringBuilder readString(InputStream inputStream) {
        return readString(inputStream, StandardCharsets.UTF_8);
    }

    /**
     * 读字符串
     *
     * @param inputStream 输入流
     * @param charset     字符集
     * @return 字符串
     */
    public static StringBuilder readString(InputStream inputStream, Charset charset) {
        return readString(getReader(inputStream, charset));
    }

    /**
     * 读字符串
     *
     * @param reader 读
     * @return 字符串
     */
    public static StringBuilder readString(Reader reader) {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(reader);
            String line;
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return stringBuilder;
    }

    /**
     * 写字符串
     *
     * @param file   文件
     * @param string 字符串
     */
    public static void writeString(File file, String string) {
        writeString(file, string, StandardCharsets.UTF_8);
    }

    /**
     * 写字符串
     *
     * @param file    文件
     * @param string  字符串
     * @param charset 字符集
     */
    public static void writeString(File file, String string, Charset charset) {
        try {
            createFile(file);
            writeString(new FileOutputStream(file), string, charset);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 写字符串
     *
     * @param outputStream 输出流
     * @param string       字符串
     */
    public static void writeString(OutputStream outputStream, String string) {
        writeString(outputStream, string, StandardCharsets.UTF_8);
    }

    /**
     * 写字符串
     *
     * @param outputStream 输出流
     * @param string       字符串
     * @param charset      字符集
     */
    public static void writeString(OutputStream outputStream, String string, Charset charset) {
        writeString(getWriter(outputStream, charset), string);
    }

    /**
     * 写字符串
     *
     * @param writer 写
     * @param string 字符串
     */
    public static void writeString(Writer writer, String string) {
        try {
            writer.write(string);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Writer getWriter(File file) {
        return getWriter(file, StandardCharsets.UTF_8);
    }

    public static Writer getWriter(File file, Charset charset) {
        try {
            createFile(file);
            return new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Writer getWriter(OutputStream outputStream) {
        return getWriter(outputStream, StandardCharsets.UTF_8);
    }

    public static Writer getWriter(OutputStream outputStream, Charset charset) {
        return new OutputStreamWriter(outputStream, charset);
    }

    public static Reader getReader(File file) {
        return getReader(file, StandardCharsets.UTF_8);
    }

    public static Reader getReader(File file, Charset charset) {
        try {
            return new InputStreamReader(new FileInputStream(file), charset);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Reader getReader(InputStream inputStream) {
        return new InputStreamReader(inputStream, StandardCharsets.UTF_8);
    }

    public static Reader getReader(InputStream inputStream, Charset charset) {
        return new InputStreamReader(inputStream, charset);
    }

    public static void createFile(File file) {
        if (!file.exists()) {
            File parentFile = file.getParentFile();
            if (parentFile.mkdirs())
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }
}