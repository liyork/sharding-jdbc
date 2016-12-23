/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.alibaba.druid.sql.lexer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * 处理词.
 *
 * @author zhangliang
 */
@RequiredArgsConstructor
public final class Term {
    
    private final String input;
    
    private final Map<String, Token> tokenDictionary;
    
    @Getter(AccessLevel.PACKAGE)
    private int offset;
    
    @Getter(AccessLevel.PACKAGE)
    private int length;
    
    @Getter(AccessLevel.PACKAGE)
    private String literals;
    
    @Getter(AccessLevel.PACKAGE)
    private Token token;
    
    public String getValue() {
        return input.substring(offset, offset + length);
    }
    
    int getCurrentPosition() {
        return offset + length;
    }
    
    void scanContentUntil(final int currentPosition, final char terminatedSign, final Token defaultToken, final boolean contentOnly) {
        offset = currentPosition;
        int position = currentPosition + 1;
        length = 2;
        while (terminatedSign != charAt(++position)) {
            if (CharTypes.EOI == charAt(position)) {
                throw new UnterminatedSignException(terminatedSign);
            }
            length++;
        }
        length++;
        literals = contentOnly ? input.substring(offset + 1, offset + length - 1) : input.substring(offset, offset + length);
        token = null == tokenDictionary.get(literals.toUpperCase()) ? defaultToken : tokenDictionary.get(literals.toUpperCase());
    }
    
    void scanVariable(final int currentPosition) {
        offset = currentPosition;
        int position = currentPosition;
        length = 1;
        if ('@' == charAt(position + 1)) {
            position++;
            length++;
        }
        while (isIdentifierChar(charAt(++position))) {
            length++;
        }
        literals = input.substring(offset, offset + length);
        token = Token.VARIANT;
    }
    
    void scanIdentifier(final int currentPosition) {
        offset = currentPosition;
        int position = currentPosition;
        length = 1;
        while (isIdentifierChar(charAt(++position))) {
            length++;
        }
        literals = input.substring(offset, offset + length);
        String upperCaseLiterals = literals.toUpperCase();
        if (Token.ORDER.getName().equals(upperCaseLiterals) || Token.GROUP.getName().equals(upperCaseLiterals)) {
            int i = 0;
            while (CharTypes.isWhitespace(charAt(position + i))) {
                i++;
            }
            if (Token.BY.getName().equalsIgnoreCase(String.valueOf(new char[] {charAt(position + i), charAt(position + i + 1)}))) {
                token = tokenDictionary.get(literals.toUpperCase());
            } else {
                token = Token.IDENTIFIER;
            }
        } else {
            token = tokenDictionary.containsKey(upperCaseLiterals) ? tokenDictionary.get(upperCaseLiterals) : Token.IDENTIFIER;
        }
    }
    
    private boolean isIdentifierChar(final char ch) {
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || '_' == ch || '$' == ch || '#' == ch;
    }
    
    void scanHexDecimal(final int currentPosition) {
        offset = currentPosition;
        int position = currentPosition + 2;
        length = 3;
        if ('-' == charAt(position)) {
            position++;
            length++;
        }
        while (isHex(charAt(++position))) {
            length++;
        }
        literals = input.substring(offset, offset + length);
        token = Token.LITERAL_HEX;
    }
    
    private boolean isHex(final char ch) {
        return (ch >= 'A' && ch <= 'F') || (ch >= 'a' && ch <= 'f') || (ch >= '0' && ch <= '9');
    }
    
    void scanNumber(final int currentPosition) {
        offset = currentPosition;
        int position = currentPosition;
        length = 0;
        if ('-' == charAt(position)) {
            position++;
            length++;
        }
        while (isDigital(charAt(position))) {
            length++;
            position++;
        }
        boolean isFloat = false;
        if ('.' == charAt(position)) {
            // TODO 待确认 数字后面加两个点表示什么
            if ('.' == charAt(position + 1)) {
                length++;
                literals = input.substring(offset, offset + length);
                token = Token.LITERAL_INT;
                return;
            }
            isFloat = true;
            position++;
            length++;
            while (isDigital(charAt(position))) {
                position++;
                length++;
            }
        }
        if ('e' == charAt(position) || 'E' == charAt(position)) {
            isFloat = true;
            position++;
            length++;
            if ('+' == charAt(position)) {
                position++;
                length++;
            }
            if ('-' == charAt(position)) {
                position++;
                length++;
            }
            while (isDigital(charAt(position))) {
                position++;
                length++;
            }
        }
        // TODO ORACLE拆分
        if ('f' == charAt(position) || 'F' == charAt(position)) {
            length++;
            literals = input.substring(offset, offset + length);
            token = Token.BINARY_FLOAT;
            return;
        }
        if ('d' == charAt(position) || 'D' == charAt(position)) {
            length++;
            literals = input.substring(offset, offset + length);
            token = Token.BINARY_DOUBLE;
            return;
        }
        literals = input.substring(offset, offset + length);
        token = isFloat ? Token.LITERAL_FLOAT : Token.LITERAL_INT;
    }
    
    private boolean isDigital(final char ch) {
        return ch >= '0' && ch <= '9';
    }
    
    void scanString(final int currentPosition) {
        offset = currentPosition;
        int position = currentPosition + 1;
        length = 1;
        while ('\'' != charAt(position) || hasEscapeChar(position)) {
            if (position >= input.length()) {
                throw new UnterminatedSignException('\'');
            }
            if (hasEscapeChar(position)) {
                length++;
                position++;
            }
            length++;
            position++;
        }
        length++;
        literals = input.substring(offset + 1, offset + length - 1);
        token = Token.LITERAL_CHARS;
    }
    
    private boolean hasEscapeChar(final int position) {
        return '\'' == charAt(position) && '\'' == charAt(position + 1);
    }
    
    void scanHint(final int currentPosition) {
        offset = currentPosition - 1;
        int position = currentPosition + 3;
        length = 4;
        while (!('*' == charAt(position) && '/' == charAt(position + 1))) {
            if (CharTypes.EOI == charAt(position)) {
                throw new UnterminatedSignException("*/");
            }
            position++;
            length++;
        }
        length += 2;
        literals = input.substring(offset + 4, offset + length - 2);
        token = Token.HINT;
    }
    
    void scanSingleLineComment(final int currentPosition, final int commentFlagLength) {
        offset = currentPosition - 1;
        int position = currentPosition + commentFlagLength;
        length = commentFlagLength + 1;
        while (CharTypes.EOI != charAt(position) && '\n' != charAt(position)) {
            position++;
            length++;
        }
        literals = input.substring(offset, offset + length);
        token = Token.LINE_COMMENT;
    }
    
    void scanMultiLineComment(final int currentPosition) {
        offset = currentPosition - 1;
        int position = currentPosition + 2;
        length = 3;
        while (!('*' == charAt(position) && '/' == charAt(position + 1))) {
            if (CharTypes.EOI == charAt(position)) {
                throw new UnterminatedSignException("*/");
            }
            position++;
            length++;
        }
        length += 2;
        literals = input.substring(offset, offset + length);
        token = Token.MULTI_LINE_COMMENT;
    }
    
    void scanSymbol(final int currentPosition, final int charLength) {
        offset = currentPosition;
        int position = currentPosition;
        length = 0;
        char[] symbolChars = new char[charLength];
        for (int i = 0; i < charLength; i++) {
            symbolChars[i] = charAt(position++);
            length++;
        }
        literals = String.valueOf(symbolChars);
        token = tokenDictionary.get(literals);
    }
    
    private char charAt(final int index) {
        return index >= input.length() ? (char) CharTypes.EOI : input.charAt(index);
    }
}