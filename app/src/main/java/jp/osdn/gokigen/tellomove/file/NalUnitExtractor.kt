package jp.osdn.gokigen.tellomove.file

import android.util.Log
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.Arrays

/**
 * 生のH.264/H.265バイトストリームからNALユニットを抽出するユーティリティクラス。
 * NALユニットはスタートコード (0x00000001 または 0x000001) で区切られていると仮定します。
 * (Gemini 2.5 Flashで生成）
 */
class NalUnitExtractor(private val inputStream: InputStream) : AutoCloseable
{
    // ----- NALユニットの検索とバッファリングのための内部バッファ
    private val searchBuffer = ByteArray(1024 * 1024) // 1MB buffer
    private var searchBufferOffset = 0 // searchBuffer内の現在の有効バイト数

    // ----- searchBuffer内で次のスタートコードのインデックスを見つける内部関数
    private fun findNextStartCode(data: ByteArray, offset: Int, limit: Int): Int
    {
        // スタートコードの十分なバイト数がない
        if (limit - offset < START_CODE_3_BYTES.size) { return -1 }
        for (i in offset until limit - START_CODE_3_BYTES.size + 1)
        {
            // 4バイトのスタートコードをチェック
            if (i + START_CODE_4_BYTES.size <= limit &&
                data[i] == START_CODE_4_BYTES[0] &&
                data[i + 1] == START_CODE_4_BYTES[1] &&
                data[i + 2] == START_CODE_4_BYTES[2] &&
                data[i + 3] == START_CODE_4_BYTES[3]) {
                return i
            }
            // 3バイトのスタートコードをチェック
            if (data[i] == START_CODE_3_BYTES[0] &&
                data[i + 1] == START_CODE_3_BYTES[1] &&
                data[i + 2] == START_CODE_3_BYTES[2]) {
                return i
            }
        }
        return (-1)
    }

    /**
     * ストリームから次のNALユニットを読み込みます。
     * @return NALユニットを含むByteBuffer (スタートコードを含む)。ストリームの終端に達した場合はnull。
     */
    fun readNextNalUnit(): ByteBuffer?
    {
        // 完全なNALユニットを見つけるか、ストリームの終端に達するまでループ
        while (true)
        {
            // 必要に応じてsearchBufferに追加データを読み込む
            val bytesToRead = searchBuffer.size - searchBufferOffset
            if (bytesToRead > 0)
            {
                val bytesRead = inputStream.read(searchBuffer, searchBufferOffset, bytesToRead)
                if (bytesRead == -1)
                {
                    // ストリームの終端、残りのデータをNALユニットとして返す
                    if (searchBufferOffset > 0)
                    {
                        val lastNal = ByteBuffer.wrap(Arrays.copyOfRange(searchBuffer, 0, searchBufferOffset))
                        searchBufferOffset = 0 // バッファをクリア
                        return lastNal
                    }
                    return (null) // これ以上データがない
                }
                searchBufferOffset += bytesRead
            }

            // 最初のスタートコードを見つける
            val firstStartCodeIdx = findNextStartCode(searchBuffer, 0, searchBufferOffset)

            if (firstStartCodeIdx == -1)
            {
                // 現在のバッファにスタートコードが見つからない
                // バッファが満杯で、まだスタートコードが見つからない場合、
                // 現在のNALユニット（またはジャンクデータ）がバッファサイズより大きいことを意味する。
                // 単純化のため、バッファ全体を「NALユニット」として返し、
                // 非常に大きなNALユニットか無効なデータであると仮定する。
                if (searchBufferOffset == searchBuffer.size)
                {
                    Log.w(TAG, "NAL unit larger than search buffer. Returning full buffer content as a NAL unit.")
                    val nalUnit = ByteBuffer.wrap(searchBuffer.copyOf(searchBufferOffset))
                    searchBufferOffset = 0 // バッファをリセット
                    return nalUnit
                }
                // そうでない場合は、さらにデータを読み込もうとする
                continue
            }

            // 最初のスタートコードが見つかった。次に、現在のNALユニットを区切る次のスタートコードを見つける
            val startCodeLength = if (firstStartCodeIdx + START_CODE_4_BYTES.size <= searchBufferOffset &&
                Arrays.copyOfRange(
                    searchBuffer,
                    firstStartCodeIdx,
                    firstStartCodeIdx + START_CODE_4_BYTES.size
                ).contentEquals(START_CODE_4_BYTES)
            )
            {
                START_CODE_4_BYTES.size
            }
            else
            {
                START_CODE_3_BYTES.size
            }

            val nextStartCodeIdx = findNextStartCode(searchBuffer, firstStartCodeIdx + startCodeLength, searchBufferOffset)

            if (nextStartCodeIdx == -1)
            {
                // 現在のバッファに次のスタートコードが見つからない。
                // このNALユニットは不完全であるか、最後のNALユニットである可能性がある。
                // 現在のデータをバッファの先頭にシフトして、さらに読み込む。
                System.arraycopy(searchBuffer, firstStartCodeIdx, searchBuffer, 0, searchBufferOffset - firstStartCodeIdx)
                searchBufferOffset -= firstStartCodeIdx
                // NALユニットを完了するためにさらにデータを読み込もうとする
                continue
            }
            else
            {
                // 次のスタートコードが見つかったため、現在のNALユニットは完全である。
                val nalUnitBytes = Arrays.copyOfRange(searchBuffer, firstStartCodeIdx, nextStartCodeIdx)

                // 残りのデータをバッファの先頭にシフトする
                System.arraycopy(searchBuffer, nextStartCodeIdx, searchBuffer, 0, searchBufferOffset - nextStartCodeIdx)
                searchBufferOffset -= nextStartCodeIdx

                return ByteBuffer.wrap(nalUnitBytes)
            }
        }
    }

    override fun close()
    {
        inputStream.close()
    }

    companion object
    {
        private val  TAG = NalUnitExtractor::class.java.simpleName
        private val START_CODE_3_BYTES = byteArrayOf(0x00, 0x00, 0x01)
        private val START_CODE_4_BYTES = byteArrayOf(0x00, 0x00, 0x00, 0x01)
    }
}
