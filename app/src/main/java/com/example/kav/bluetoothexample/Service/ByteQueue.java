package com.example.kav.bluetoothexample.Service;

import android.support.annotation.NonNull;

public class ByteQueue
{
  private byte[] pipe;
  private int writeIndex;
  private int readIndex;
  private int top;

  public ByteQueue(int limit)
  {
    pipe = new byte[limit];
  }

  public void clear()
  {
    top = writeIndex = readIndex = 0;
  }

  private void r(@NonNull byte[] container, int pos, int length)
  {
    System.arraycopy(pipe, readIndex, container, pos, length);
    readIndex += length;
  }

  public int read(@NonNull byte[] container)
  {
    int readLength = 0;
    if (readIndex > writeIndex) {
      if (readIndex < top) {
        readLength = Math.min(top - readIndex, container.length);
        r(container, 0, readLength);
      }
      if (readIndex >= top)
        readIndex = 0;
    }
    if (readLength < container.length && writeIndex - readIndex > 0) {
      int L = Math.min(container.length - readLength, writeIndex - readIndex);
      r(container, readLength, L);
      readLength += L;
    }
    return readLength;
  }

  private void w(@NonNull byte[] data)
  {
    System.arraycopy(data, 0, pipe, writeIndex, data.length);
    writeIndex += data.length;
  }

  public boolean write(@NonNull byte[] data)
  {
    if (writeIndex < readIndex) {
      if (data.length + 1 < readIndex - writeIndex) {
        w(data);
        return true;
      }
      return false;
    }
    if (data.length >= pipe.length - writeIndex && data.length < readIndex)
      writeIndex = 0;
    if (data.length < pipe.length - writeIndex) {
      w(data);
      top = writeIndex;
      return true;
    }
    return false;
  }
}
