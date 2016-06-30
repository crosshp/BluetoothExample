package com.example.kav.bluetoothexample.Service;

import android.support.annotation.NonNull;

public class Hex
{
  private static final char[] HexCharLookup = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

  @NonNull
  public static String toString(@NonNull byte[] bytes)
  {
    char[] c = new char[bytes.length * 2];
    for (int i = 0, ci = 0; i < bytes.length; i++) {
      c[ci++] = HexCharLookup[(bytes[i] & 0xff) >> 4];
      c[ci++] = HexCharLookup[bytes[i] & 0x0f];
    }
    return String.valueOf(c);
  }

  @NonNull
  public static String toString(@NonNull byte[] buffer, int pos, int length)
  {
    if (length + pos > buffer.length)
      throw new IllegalArgumentException("Length must be less than buffer size");
    char[] c = new char[length * 2];
    for (int i = pos, ci = 0; ci < c.length; i++) {
      c[ci++] = HexCharLookup[(buffer[i] & 0xff) >> 4];
      c[ci++] = HexCharLookup[buffer[i] & 0x0f];
    }
    return String.valueOf(c);
  }

  public static byte[] toByteArray(@NonNull String hex)
  {
    int L = hex.length();
    if (L % 2 != 0)
      throw new IllegalArgumentException("Length must be even");
    byte[] result = new byte[L / 2];
    for(int i = 0; i < L; i += 2)
      result[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i+1), 16));
    return result;
  }
}
