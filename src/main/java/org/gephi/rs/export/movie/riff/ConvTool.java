package org.gephi.rs.export.movie.riff;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * little endian から big endian への変換。<br/>
 * little endianでunsignedの値を持つRIFFフォーマットをJavaで入出力するためのヘルパークラスです。<br/>
 * RIFFはバイナリフォーマットで、値にはunsigned longが多く使用されています。<br/>
 * @author abe
 */
public class ConvTool {
    //
    // unsigned long(C++) -> long(Java)
    //

    /**
     * Little endian の 4byte (unsigned long) から Javaのlong値に変換。<br/>
     * @param src arg0 + 4byteまで読み込み
     * @param arg0 srcの読み込み開始位置
     * @return
     */
    public static long hexToULong(byte[] src, int arg0) {
        String hex = Integer.toHexString(ByteBuffer.wrap(src).order(ByteOrder.LITTLE_ENDIAN).getInt(arg0));
        return Long.valueOf(hex.toString(), 16);
    }
    /**
     * Little endian の 4byte (unsigned long) から Javaのlong値に変換。<br/>
     * @param src 4byteまで読み込み
     * @return
     */
    public static long hexToULong(byte[] src) {
        return hexToULong(src, 0);
    }
    /**
     * int より Little endian の 4byte (unsinged long) に変換。<br/>
     * @param src 元値
     * @param dest arg0 + 4byteまで結果出力
     * @param arg0 destの書き込み開始位置
     */
    public static void ulongToHex(long src, byte[] dest, int arg0) {
        for(int i=0; i<4; i++)
            dest[arg0+i] = (byte)((src >>> (i*8)) & 0xff);
    }
    /**
     * long より Little endian の 4byte (unsinged long) に変換。<br/>
     * @param src 元値
     * @param dest 4byteまで結果出力
     */
    public static void ulongToHex(long src, byte[] dest) {
        ulongToHex(src, dest, 0);
    }

    //
    // long(C++) -> int(Java)
    //

    /**
     * Little endian の 4byte (long) から Javaのint値に変換。<br/>
     * @param src arg0 + 4byteまで読み込み
     * @param arg0 srcの読み込み開始位置
     * @return
     */
    public static int hexToLong(byte[] src, int arg0) {
        return ByteBuffer.wrap(src).order(ByteOrder.LITTLE_ENDIAN).getInt(arg0);
    }
    /**
     * Little endian の 4byte (long) から Javaのlong値に変換。<br/>
     * @param src 4byteまで読み込み
     * @return
     */
    public static int hexToLong(byte[] src) {
        return hexToLong(src, 0);
    }
    /**
     * int より Little endian の 4byte (long) に変換。<br/>
     * @param src 元値
     * @param dest arg0 + 4byteまで結果出力
     * @param arg0 destの書き込み開始位置
     */
    public static void longToHex(int src, byte[] dest, int arg0) {
        ByteBuffer.wrap(dest).order(ByteOrder.LITTLE_ENDIAN).putInt(arg0, src);
    }
    /**
     * int より Little endian の 4byte (long) に変換。<br/>
     * @param src 元値
     * @param dest 4byteまで結果出力
     */
    public static void longToHex(int src, byte[] dest) {
        longToHex(src, dest, 0);
    }

    //
    // unsigned short(C++) -> int(Java)
    //

    /**
     * Little endian の 2byte (unsigned short) から Javaのint値に変換。<br/>
     * @param src arg0 + 2byteまで読み込み
     * @param arg0 srcの読み込み開始位置
     * @return
     */
    public static int hexToUInt(byte[] src, int arg0) {
        String hex = Integer.toHexString(ByteBuffer.wrap(src).order(ByteOrder.LITTLE_ENDIAN).getShort(arg0));
        return Integer.valueOf(hex.toString(), 16);
    }
    /**
     * Little endian の 2byte (unsigned short) から Javaのint値に変換。<br/>
     * @param src 2byteまで読み込み
     * @return
     */
    public static int hexToUInt(byte[] src) {
        return hexToUInt(src, 0);
    }
    /**
     * int より Little endian の 2byte (unsinged short) に変換。<br/>
     * @param src 元値
     * @param dest arg0 + 2byteまで結果出力
     * @param arg0 destの書き込み開始位置
     */
    public static void uintToHex(long src, byte[] dest, int arg0) {
        for(int i=0; i<2; i++)
            dest[arg0+i] = (byte)((src >>> (i*8)) & 0xff);
    }
    /**
     * long より Little endian の 2byte (unsinged short) に変換。<br/>
     * @param src 元値
     * @param dest 4byteまで結果出力
     */
    public static void uintToHex(long src, byte[] dest) {
        uintToHex(src, dest, 0);
    }


    //
    // short(C++) -> short(Java)
    //

    /**
     * Little endian の 2byte (short) から Javaのshort値に変換。<br/>
     * @param src arg0 + 2byteまで読み込み
     * @param arg0 srcの読み込み開始位置
     * @return
     */
    public static short hexToShort(byte[] src, int arg0) {
        return ByteBuffer.wrap(src).order(ByteOrder.LITTLE_ENDIAN).getShort(arg0);
    }
    /**
     * Little endian の 2byte (short) から Javaのshort値に変換。<br/>
     * @param src 2byteまで読み込み
     * @return
     */
    public static short hexToShort(byte[] src) {
        return hexToShort(src, 0);
    }
    /**
     * int より Little endian の 4byte (long) に変換。<br/>
     * @param src 元値
     * @param dest arg0 + 4byteまで結果出力
     * @param arg0 destの書き込み開始位置
     */
    public static void shortToHex(short src, byte[] dest, int arg0) {
        ByteBuffer.wrap(dest).order(ByteOrder.LITTLE_ENDIAN).putShort(arg0, src);
    }
    /**
     * long より Little endian の 4byte (unsinged long) に変換。<br/>
     * @param src 元値
     * @param dest 4byteまで結果出力
     */
    public static void shortToHex(short src, byte[] dest) {
        shortToHex(src, dest, 0);
    }

    public static String getString(byte[] src, int arg0, int len) throws UnsupportedEncodingException {
        byte[] src2 = new byte[len];
        System.arraycopy(src, arg0, src2, 0, len);
        return new String(src2, "US-ASCII");
    }

    public static byte[] getAscii(String src) throws UnsupportedEncodingException {
        return src.getBytes("US-ASCII");
    }
}
